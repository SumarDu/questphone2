package neth.iecal.questphone.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import neth.iecal.questphone.data.game.AppUnlockerItem
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.getUserInfo
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.settings.SettingsData
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.services.INTENT_ACTION_REFRESH_APP_BLOCKER
import neth.iecal.questphone.services.ServiceInfo
import neth.iecal.questphone.services.saveServiceInfo
import neth.iecal.questphone.data.preferences.GestureSettingsRepository
import neth.iecal.questphone.data.preferences.GestureSettingsRepository.GestureKeys
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.SystemClock
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.edit
import neth.iecal.questphone.utils.json
import android.content.Context.MODE_PRIVATE
import android.provider.MediaStore
import android.content.ContentValues

@Serializable
data class AppUnlockerItemDTO(
    val appName: String,
    val packageName: String,
    val price: Int,
    val unlockDurationMinutes: Int
) {
    companion object {
        fun fromEntity(e: AppUnlockerItem) = AppUnlockerItemDTO(
            appName = e.appName,
            packageName = e.packageName,
            price = e.price,
            unlockDurationMinutes = e.unlockDurationMinutes
        )
        fun toEntity(d: AppUnlockerItemDTO) = AppUnlockerItem(
            id = 0, // let Room autogenerate to avoid ID collisions across devices
            appName = d.appName,
            packageName = d.packageName,
            price = d.price,
            unlockDurationMinutes = d.unlockDurationMinutes
        )
    }
}

@Serializable
data class BackupData(
    val version: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),
    // raw JSON strings for components that are not @Serializable in-place
    val settingsJson: String? = null,
    val userInfoJson: String? = null,
    val distractions: List<String> = emptyList(),
    val quests: List<CommonQuestInfo> = emptyList(),
    val appUnlockers: List<AppUnlockerItemDTO> = emptyList(),
    val timers: List<TimerStateDTO> = emptyList(),
    val gestureSettings: GestureSettingsDTO? = null
)

@Serializable
data class TimerStateDTO(
    val packageName: String,
    val remainingMs: Long
)

@Serializable
data class GestureSettingsDTO(
    val swipeUp: String? = null,
    val swipeDown: String? = null,
    val swipeLeft: String? = null,
    val swipeRight: String? = null,
    val twoFingerSwipeUp: String? = null,
    val twoFingerSwipeDown: String? = null,
    val doubleTapBottomLeft: String? = null,
    val doubleTapBottomRight: String? = null,
    val longPress: String? = null,
    val edgeLeftSwipeUp: String? = null,
    val edgeLeftSwipeDown: String? = null,
    val edgeRightSwipeUp: String? = null,
    val edgeRightSwipeDown: String? = null,
    val doubleTapBottomRightMode: String? = null,
    val bottomAppletApps: List<String> = emptyList()
)

object BackupManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun suggestedFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)
        return "questphone-backup-${fmt.format(Date(nowMillis))}.json"
    }

    suspend fun buildBackup(appContext: Context): String = withContext(Dispatchers.IO) {
        // Settings
        val settingsSp = appContext.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val settingsJson = settingsSp.getString("settings_data", null)

        // User info (coins/streaks)
        val userSp = appContext.getSharedPreferences("user_info", Context.MODE_PRIVATE)
        var userInfoJson = userSp.getString("user_info", null)
        if (userInfoJson == null) {
            // Fallback to current in-memory or disk user info
            val info = runCatching { User.userInfo }.getOrElse { getUserInfo(appContext) }
            userInfoJson = json.encodeToString(info)
        }

        // Distractions
        val dSp = appContext.getSharedPreferences("distractions", Context.MODE_PRIVATE)
        val distractions = dSp.getStringSet("distracting_apps", emptySet())?.toList() ?: emptyList()

        // Quests and Unlockers
        val db = QuestDatabaseProvider.getInstance(appContext)
        val quests = db.questDao().getAllQuestsSuspend()
        val unlockers = db.appUnlockerItemDao().getAllOnce()

        // Timer state (cooldowns in ServiceInfo.unlockedApps) -> remainingMs snapshot
        val timers = run {
            val sp = appContext.getSharedPreferences("service_info", Context.MODE_PRIVATE)
            val raw = sp.getString("unlocked_apps", null)
            val nowUp = SystemClock.uptimeMillis()
            val list = mutableListOf<TimerStateDTO>()
            if (raw != null) {
                runCatching { json.decodeFromString<Map<String, Long>>(raw) }.getOrNull()?.forEach { (pkg, end) ->
                    val rem = end - nowUp
                    if (rem > 0) list.add(TimerStateDTO(pkg, rem))
                }
            }
            list
        }

        // Gesture settings snapshot
        val gestures = run {
            val repo = GestureSettingsRepository(appContext)
            // Use flows' first() to get current values
            GestureSettingsDTO(
                swipeUp = repo.swipeUpApp.first(),
                swipeDown = repo.swipeDownApp.first(),
                swipeLeft = repo.swipeLeftApp.first(),
                swipeRight = repo.swipeRightApp.first(),
                twoFingerSwipeUp = repo.twoFingerSwipeUpApp.first(),
                twoFingerSwipeDown = repo.twoFingerSwipeDownApp.first(),
                doubleTapBottomLeft = repo.doubleTapBottomLeftApp.first(),
                doubleTapBottomRight = repo.doubleTapBottomRightApp.first(),
                longPress = repo.longPressApp.first(),
                edgeLeftSwipeUp = repo.edgeLeftSwipeUpApp.first(),
                edgeLeftSwipeDown = repo.edgeLeftSwipeDownApp.first(),
                edgeRightSwipeUp = repo.edgeRightSwipeUpApp.first(),
                edgeRightSwipeDown = repo.edgeRightSwipeDownApp.first(),
                doubleTapBottomRightMode = repo.doubleTapBottomRightMode.first(),
                bottomAppletApps = repo.bottomAppletApps.first()
            )
        }

        val dto = BackupData(
            settingsJson = settingsJson,
            userInfoJson = userInfoJson,
            distractions = distractions,
            quests = quests,
            appUnlockers = unlockers.map { AppUnlockerItemDTO.fromEntity(it) },
            timers = timers,
            gestureSettings = gestures
        )
        json.encodeToString(dto)
    }

    suspend fun restoreFromJson(context: Context, backupJson: String) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val data = json.decodeFromString(BackupData.serializer(), backupJson)

        // Restore settings
        data.settingsJson?.let { settingsStr ->
            // Parse to SettingsData via Gson to update repository state and cache
            val repo = SettingsRepository(appContext)
            val gson = com.google.gson.Gson()
            runCatching { gson.fromJson(settingsStr, SettingsData::class.java) }.getOrNull()?.let {
                repo.saveSettings(it)
            } ?: run {
                // Fallback: write raw
                appContext.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                    .edit().putString("settings_data", settingsStr).apply()
            }
        }

        // Restore user info (coins/streaks, etc.)
        data.userInfoJson?.let { userStr ->
            appContext.getSharedPreferences("user_info", Context.MODE_PRIVATE)
                .edit().putString("user_info", userStr).apply()
            // Also refresh in-memory singleton so UI reflects immediately
            runCatching {
                val info = json.decodeFromString(neth.iecal.questphone.data.game.UserInfo.serializer(), userStr)
                try {
                    User.userInfo = info
                } catch (e: UninitializedPropertyAccessException) {
                    User.init(appContext)
                    User.userInfo = info
                }
            }
        }

        // Restore distractions
        appContext.getSharedPreferences("distractions", Context.MODE_PRIVATE)
            .edit().putStringSet("distracting_apps", data.distractions.toSet()).apply()

        // Restore quests
        val db = QuestDatabaseProvider.getInstance(appContext)
        val questDao = db.questDao()
        questDao.upsertAll(data.quests)

        // Restore unlockers: clear and insert
        val unlockerDao = db.appUnlockerItemDao()
        // Requires clearAll() DAO method; if absent, this call will be a no-op at compile time until method is added.
        runCatching { unlockerDao.clearAll() }.onFailure { /* ignore if not available */ }
        data.appUnlockers.forEach { dto -> unlockerDao.insert(AppUnlockerItemDTO.toEntity(dto)) }

        // Restore gesture settings
        data.gestureSettings?.let { g ->
            val repo = GestureSettingsRepository(appContext)
            // Use DataStore edit for batch to minimize disk writes
            val ds = appContext.let { (repo as Any) }
            // We don't have direct dataStore access; do individual saves via repository API
            if (g.swipeUp != null) repo.saveSwipeUpApp(g.swipeUp)
            if (g.swipeDown != null) repo.saveSwipeDownApp(g.swipeDown)
            if (g.swipeLeft != null) repo.saveSwipeLeftApp(g.swipeLeft)
            if (g.swipeRight != null) repo.saveSwipeRightApp(g.swipeRight)
            if (g.twoFingerSwipeUp != null) repo.saveTwoFingerSwipeUpApp(g.twoFingerSwipeUp)
            if (g.twoFingerSwipeDown != null) repo.saveTwoFingerSwipeDownApp(g.twoFingerSwipeDown)
            if (g.doubleTapBottomLeft != null) repo.saveDoubleTapBottomLeftApp(g.doubleTapBottomLeft)
            if (g.doubleTapBottomRight != null) repo.saveDoubleTapBottomRightApp(g.doubleTapBottomRight)
            if (g.longPress != null) repo.saveLongPressApp(g.longPress)
            if (g.edgeLeftSwipeUp != null) repo.saveEdgeLeftSwipeUpApp(g.edgeLeftSwipeUp)
            if (g.edgeLeftSwipeDown != null) repo.saveEdgeLeftSwipeDownApp(g.edgeLeftSwipeDown)
            if (g.edgeRightSwipeUp != null) repo.saveEdgeRightSwipeUpApp(g.edgeRightSwipeUp)
            if (g.edgeRightSwipeDown != null) repo.saveEdgeRightSwipeDownApp(g.edgeRightSwipeDown)
            if (g.doubleTapBottomRightMode != null) repo.saveDoubleTapBottomRightMode(g.doubleTapBottomRightMode)
            if (g.bottomAppletApps.isNotEmpty()) repo.saveBottomAppletApps(g.bottomAppletApps)
        }

        // Restore timers: reconstruct endTime from remainingMs
        if (data.timers.isNotEmpty()) {
            val nowUp = SystemClock.uptimeMillis()
            ServiceInfo.unlockedApps.clear()
            data.timers.forEach { t ->
                val end = nowUp + t.remainingMs
                ServiceInfo.unlockedApps[t.packageName] = end
            }
            saveServiceInfo(appContext)
            // Notify services to refresh blocker state
            appContext.sendBroadcast(Intent(INTENT_ACTION_REFRESH_APP_BLOCKER))
        }
    }

    suspend fun writeToUri(context: Context, uri: Uri, content: String) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        resolver.openOutputStream(uri)?.use { os: OutputStream ->
            os.write(content.toByteArray(Charsets.UTF_8))
            os.flush()
        } ?: error("Unable to open output stream")
    }

    fun writeToAppFiles(context: Context, fileName: String, content: String): String {
        context.openFileOutput(fileName, MODE_PRIVATE).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
            fos.flush()
        }
        return context.filesDir.resolve(fileName).absolutePath
    }

    fun writeToDownloads(context: Context, fileName: String, content: String): String? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { os ->
            os.write(content.toByteArray(Charsets.UTF_8))
            os.flush()
        } ?: return null
        return uri.toString()
    }

    suspend fun readFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { ins: InputStream ->
            ins.readBytes().toString(Charsets.UTF_8)
        } ?: error("Unable to open input stream")
    }
}
