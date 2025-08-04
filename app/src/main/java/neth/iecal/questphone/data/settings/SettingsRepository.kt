package neth.iecal.questphone.data.settings

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import neth.iecal.questphone.utils.CalendarSyncScheduler

data class SettingsData(
    val isEditingEnabled: Boolean = true,
    val isQuestCreationEnabled: Boolean = true,
    val isQuestDeletionEnabled: Boolean = true,
    val isItemCreationEnabled: Boolean = true,
    val isItemDeletionEnabled: Boolean = true,
    val isSettingsLocked: Boolean = false,
    val settingsLockPassword: String? = null,
    val settingsLockoutEndDate: Long? = null,
    val geminiApiKey: String? = null,
    val autoSyncTimeMinutes: Int? = null, // Time of day for auto-sync in minutes from midnight, null means disabled
    val selectedCalendars: Set<String> = emptySet(), // Set of calendar IDs to sync from
    // Quest filter settings for HomeScreen + dialog
    val showRepeatingQuestsInDialog: Boolean = true,
    val showClonedQuestsInDialog: Boolean = true,
    val showOneTimeQuestsInDialog: Boolean = true,
    val unplannedBreakReasons: List<String> = emptyList()
)

class SettingsRepository(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // In-memory cache
    private var cachedSettings: SettingsData? = null
    private val _settings = MutableStateFlow(loadSettings())
    val settings = _settings.asStateFlow()

    private fun loadSettings(): SettingsData {
        // Check memory cache first
        cachedSettings?.let { return it }

        val json = sharedPreferences.getString("settings_data", null)
        return if (json != null) {
            gson.fromJson(json, SettingsData::class.java).also {
                cachedSettings = it
            }
        } else {
            SettingsData().also {
                cachedSettings = it
            }
        }
    }

    suspend fun saveSettings(settingsData: SettingsData) {
        withContext(Dispatchers.IO) {
        val json = gson.toJson(settingsData)
            sharedPreferences.edit()
                .putString("settings_data", json)
                .apply()
            
            cachedSettings = settingsData
        _settings.value = settingsData
        }
    }

    suspend fun updateQuestCreation(isEnabled: Boolean) {
        val newSettings = _settings.value.copy(isQuestCreationEnabled = isEnabled)
        saveSettings(newSettings)
    }

    suspend fun updateQuestDeletion(isEnabled: Boolean) {
        val newSettings = _settings.value.copy(isQuestDeletionEnabled = isEnabled)
        saveSettings(newSettings)
    }

    suspend fun updateItemCreation(isEnabled: Boolean) {
        val newSettings = _settings.value.copy(isItemCreationEnabled = isEnabled)
        saveSettings(newSettings)
    }

    suspend fun updateItemDeletion(isEnabled: Boolean) {
        val newSettings = _settings.value.copy(isItemDeletionEnabled = isEnabled)
        saveSettings(newSettings)
    }

    suspend fun updateEditingPermission(isEnabled: Boolean) {
        val newSettings = _settings.value.copy(
            isEditingEnabled = isEnabled,
            isQuestCreationEnabled = isEnabled,
            isQuestDeletionEnabled = isEnabled,
            isItemCreationEnabled = isEnabled,
            isItemDeletionEnabled = isEnabled
        )
        saveSettings(newSettings)
    }

    suspend fun updateSettingsLock(isLocked: Boolean, password: String?, lockoutEndDate: Long?) {
        val newSettings = _settings.value.copy(
            isSettingsLocked = isLocked,
            settingsLockPassword = password,
            settingsLockoutEndDate = lockoutEndDate
        )
        saveSettings(newSettings)
    }

    suspend fun updateGeminiApiKey(apiKey: String) {
        val newSettings = _settings.value.copy(geminiApiKey = apiKey)
        saveSettings(newSettings)
    }

    suspend fun updateAutoSyncTime(minutes: Int?) {
        val newSettings = _settings.value.copy(autoSyncTimeMinutes = minutes)
        saveSettings(newSettings)

        // Schedule or cancel automatic sync based on the new time setting
        CalendarSyncScheduler.scheduleSync(context, minutes)
    }

    suspend fun updateSelectedCalendars(calendarIds: Set<String>) {
        val newSettings = _settings.value.copy(selectedCalendars = calendarIds)
        saveSettings(newSettings)
    }

    suspend fun updateShowRepeatingQuestsInDialog(show: Boolean) {
        val newSettings = _settings.value.copy(showRepeatingQuestsInDialog = show)
        saveSettings(newSettings)
    }

    suspend fun updateShowClonedQuestsInDialog(show: Boolean) {
        val newSettings = _settings.value.copy(showClonedQuestsInDialog = show)
        saveSettings(newSettings)
    }

    suspend fun updateShowOneTimeQuestsInDialog(show: Boolean) {
        val newSettings = _settings.value.copy(showOneTimeQuestsInDialog = show)
        saveSettings(newSettings)
    }

    suspend fun updateUnplannedBreakReasons(reasons: List<String>) {
        val newSettings = _settings.value.copy(unplannedBreakReasons = reasons)
        saveSettings(newSettings)
    }
}
