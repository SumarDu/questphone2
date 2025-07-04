package neth.iecal.questphone.ui.screens.quest.view

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.getUserInfo
import neth.iecal.questphone.data.game.xpToRewardForQuest
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.focus.DeepFocus
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsInfo
import neth.iecal.questphone.services.AppBlockerService
import neth.iecal.questphone.services.INTENT_ACTION_START_DEEP_FOCUS
import neth.iecal.questphone.services.INTENT_ACTION_STOP_DEEP_FOCUS
import neth.iecal.questphone.services.ServiceInfo
import neth.iecal.questphone.ui.screens.quest.checkForRewards
import neth.iecal.questphone.ui.screens.quest.view.components.MdPad
import neth.iecal.questphone.ui.theme.JetBrainsMonoFont
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.formatHour
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.json
import neth.iecal.questphone.utils.sendRefreshRequest
import neth.iecal.questphone.data.timer.TimerService
import java.util.UUID

private const val PREF_NAME = "deep_focus_prefs"
private const val KEY_START_TIME = "start_time_"
private const val KEY_PAUSED_ELAPSED = "paused_elapsed_"
private const val NOTIFICATION_CHANNEL_ID = "focus_timer_channel"
private const val NOTIFICATION_ID = 1001

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeepFocusQuestView(
    commonQuestInfo: CommonQuestInfo
) {
    val context = LocalContext.current
    val questHelper = QuestHelper(context)
    val deepFocus = json.decodeFromString<DeepFocus>(commonQuestInfo.quest_json)
    val duration = deepFocus.nextFocusDurationInMillis
    val isInTimeRange = remember { mutableStateOf(QuestHelper.Companion.isInTimeRange(commonQuestInfo)) }
    val timerState by TimerService.timerState.collectAsState()


    LaunchedEffect(Unit) {
        createNotificationChannel(context)
    }


    var isQuestComplete = remember {
        mutableStateOf(commonQuestInfo.last_completed_on == getCurrentDate())
    }
    var isQuestRunning by remember {
        mutableStateOf(questHelper.isQuestRunning(commonQuestInfo.title))
    }

    var timerActive by remember { mutableStateOf(false) }

    val questKey = commonQuestInfo.title.replace(" ", "_").lowercase()

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }

    val isFailed = remember { mutableStateOf(questHelper.isOver(commonQuestInfo)) }

    val dao = QuestDatabaseProvider.getInstance(context).questDao()
    val scope = rememberCoroutineScope()
    var progress by remember {
        mutableFloatStateOf(if (isQuestComplete.value || isFailed.value ) 1f else 0f)
    }
    val startTimeKey = KEY_START_TIME + questKey
    val pausedElapsedKey = KEY_PAUSED_ELAPSED + questKey
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Observe app lifecycle for notification management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isAppInForeground = true
                    cancelTimerNotification(context)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isAppInForeground = false
                    // Show notification if quest is running and app goes to background
                    if (isQuestRunning && !isQuestComplete.value) {
                        updateTimerNotification(context, commonQuestInfo.title, progress, duration)
                    }
                }
                else -> { /* Ignore other events */ }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val userInfo = getUserInfo(LocalContext.current)

    fun onQuestComplete(){
        deepFocus.incrementTime()
        commonQuestInfo.last_completed_on = getCurrentDate()
        commonQuestInfo.quest_json = json.encodeToString(deepFocus)
        commonQuestInfo.synced = false
        commonQuestInfo.last_updated = System.currentTimeMillis()
        scope.launch {
            dao.upsertQuest(commonQuestInfo)

            val statsDao = StatsDatabaseProvider.getInstance(context).statsDao()
            statsDao.upsertStats(
                StatsInfo(
                    id = UUID.randomUUID().toString(),
                    quest_id = commonQuestInfo.id,
                    user_id = User.userInfo.id,
                )
            )
        }

        questHelper.setQuestRunning(commonQuestInfo.title, false)
        checkForRewards(commonQuestInfo)
        isQuestRunning = false
        timerActive = false

        progress = 1f
        // Clear saved times
        prefs.edit {
            remove(startTimeKey)
                .remove(pausedElapsedKey)
        }

        // Cancel notification when complete
        cancelTimerNotification(context)
        sendRefreshRequest(context, INTENT_ACTION_STOP_DEEP_FOCUS)

        ServiceInfo.deepFocus.isRunning = false
        isQuestComplete.value =true
    }
    fun startQuest() {
        questHelper.setQuestRunning(commonQuestInfo.title, true)
        isQuestRunning = true
        timerActive = true

        if(!ServiceInfo.isUsingAccessibilityService && ServiceInfo.appBlockerService==null){
            startForegroundService(context,Intent(context, AppBlockerService::class.java))
        }
        // Clear any existing data and set fresh start time
        prefs.edit {
            putLong(KEY_START_TIME + questKey, System.currentTimeMillis())
                .putLong(KEY_PAUSED_ELAPSED + questKey, 0L)
        }
        ServiceInfo.deepFocus.isRunning = true
        ServiceInfo.deepFocus.exceptionApps = deepFocus.unrestrictedApps.toHashSet()
        val intent = Intent(INTENT_ACTION_START_DEEP_FOCUS)
        intent.putStringArrayListExtra("exception", deepFocus.unrestrictedApps.toCollection(ArrayList()))
        context.sendBroadcast(intent)
    }

    // Load initial state
    LaunchedEffect(Unit) {
        if (isQuestRunning && !isQuestComplete.value) {
            timerActive = true
        }
    }

    // Handle the timer - use timerActive state to trigger/stop
    LaunchedEffect(timerActive) {
        if (timerActive) {
            // Get the start time from SharedPreferences or use current time if not found
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)


            // Get saved values or use defaults
            val savedStartTime = prefs.getLong(startTimeKey, 0L)
            val pausedElapsed = prefs.getLong(pausedElapsedKey, 0L)

            val startTime = if (savedStartTime == 0L) {
                // First time starting the timer
                val newStartTime = System.currentTimeMillis() - pausedElapsed
                prefs.edit { putLong(startTimeKey, newStartTime) }
                newStartTime
            } else {
                // Resuming existing timer
                savedStartTime
            }

            // Update progress continually
            while (progress < 1f && timerActive) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime
                progress = (elapsedTime / duration.toFloat()).coerceIn(0f, 1f)

                // Update notification if app is in background
                if (!isAppInForeground && isQuestRunning) {
                    updateTimerNotification(context, commonQuestInfo.title, progress, duration)
                }

                if (progress >= 1f) {
                    isQuestRunning = false
                    onQuestComplete()
                }

                delay(1000) // Update every second instead of 100ms to reduce battery usage
            }
        }
    }
    LaunchedEffect(progress) {
        if (progress >= 1f && isQuestRunning) {
            onQuestComplete()
        }
    }

    // Save state when leaving the composition
    DisposableEffect(Unit) {
        onDispose {
            if (isQuestRunning && progress < 1f) {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val startTimeKey = KEY_START_TIME + questKey
                val savedStartTime = prefs.getLong(startTimeKey, 0L)

                if (savedStartTime > 0) {
                    val pausedElapsedKey = KEY_PAUSED_ELAPSED + questKey
                    val elapsedTime = System.currentTimeMillis() - savedStartTime
                    prefs.edit { putLong(pausedElapsedKey, elapsedTime) }
                }
            }
        }
    }

    // Prevent back navigation when quest is running
    BackHandler(isQuestRunning) {}

    // Convert progress state to MutableState<Float> for BaseQuestView
    val progressState = remember(progress) { mutableFloatStateOf(progress) }

    BaseQuestView(
        hideStartQuestBtn = isQuestComplete.value || isQuestRunning || isFailed.value || !isInTimeRange.value,
        progress = progressState,
        isFailed = isFailed,
        onQuestStarted = {
            // Start the quest immediately - this is called when button is pressed
            startQuest()
        },
         onQuestCompleted = {onQuestComplete()},
        isQuestCompleted = isQuestComplete) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = commonQuestInfo.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                fontFamily = JetBrainsMonoFont,
                modifier = Modifier.padding(top = 40.dp)
            )

            val rewardText = if (commonQuestInfo.reward_min == commonQuestInfo.reward_max) {
                "${commonQuestInfo.reward_min} coins"
            } else {
                "${commonQuestInfo.reward_min}-${commonQuestInfo.reward_max} coins"
            }
            Text(
                text = (if(isQuestComplete.value) "Reward" else "Next Reward") + ": $rewardText + ${
                    xpToRewardForQuest(
                        userInfo.level
                    )
                } xp",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
            )

            if(!isInTimeRange.value){
                Text(
                    text = "Time: ${formatHour(commonQuestInfo.time_range[0])} to ${formatHour(commonQuestInfo.time_range[1])}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )
            }
            // Show remaining time
            if (isQuestRunning && progress < 1f) {
                val remainingSeconds = ((duration * (1 - progress)) / 1000).toInt()
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60

                Text(
                    text = "Remaining: $minutes:${seconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = timerState.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                modifier = Modifier.padding(top = 32.dp)
            )

            Text(
                text = if (!isQuestComplete.value) "Duration: ${duration / 60_000}m" else "Next Duration: ${duration / 60_000}m",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                modifier = Modifier.padding(top = 32.dp)
            )

            val pm = context.packageManager
            val apps = deepFocus.unrestrictedApps.mapNotNull { packageName ->
                try {
                    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() to packageName
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Unrestricted Apps: ",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                )
                apps.forEach { (appName, packageName) ->
                    Text(
                        text = "$appName, ",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                        modifier = Modifier
                            .clickable {
                                // Show notification before launching other app
                                if (isQuestRunning && !isQuestComplete.value) {
                                    updateTimerNotification(context, commonQuestInfo.title, progress, duration)
                                }
                                val intent = pm.getLaunchIntentForPackage(packageName)
                                intent?.let { context.startActivity(it) }
                            }
                    )
                }
            }
            MdPad(commonQuestInfo)

        }
    }
}

private fun createNotificationChannel(context: Context) {
    val name = "Focus Timer"
    val descriptionText = "Shows the remaining time for focus quests"
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
        description = descriptionText
        setShowBadge(true)
    }
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

private fun updateTimerNotification(context: Context, questTitle: String, progress: Float, duration: Long) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val remainingSeconds = ((duration * (1 - progress)) / 1000).toInt()
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "$minutes:${seconds.toString().padStart(2, '0')}"

    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_lock_idle_alarm) // Use your app's icon here
        .setContentTitle("Focus Quest: $questTitle")
        .setContentText("Remaining time: $timeText")
        .setProgress(100, (progress * 100).toInt(), false)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOngoing(true) // Make it persistent
        .setContentIntent(pendingIntent)
        .setSilent(true)
        .setAutoCancel(false)
        .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
}

private fun cancelTimerNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(NOTIFICATION_ID)
}