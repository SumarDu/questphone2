package launcher.launcher.ui.screens.quest.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.data.quest.focus.DeepFocus
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.getCurrentDate

private const val PREF_NAME = "deep_focus_prefs"
private const val KEY_START_TIME = "start_time_"
private const val KEY_PAUSED_ELAPSED = "paused_elapsed_"
private const val NOTIFICATION_CHANNEL_ID = "focus_timer_channel"
private const val NOTIFICATION_ID = 1001

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeepFocusQuestView(
    basicQuestInfo: BasicQuestInfo
) {
    val context = LocalContext.current
    val questHelper = QuestHelper(context)
    val deepFocus = questHelper.getQuestInfo<DeepFocus>(basicQuestInfo) ?: return
    val duration = deepFocus.nextFocusDurationInMillis

    createNotificationChannel(context)

    var isQuestComplete by remember {
        mutableStateOf(questHelper.isQuestCompleted(basicQuestInfo.title, getCurrentDate()) ?: false)
    }
    var isQuestRunning by remember {
        mutableStateOf(questHelper.isQuestRunning(basicQuestInfo.title))
    }

    var timerActive by remember { mutableStateOf(false) }

    var progress by remember {
        mutableFloatStateOf(if(isQuestComplete) 1f else 0f)
    }

    val questKey = basicQuestInfo.title.replace(" ", "_").lowercase()

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }

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
                    if (isQuestRunning && !isQuestComplete) {
                        updateTimerNotification(context, basicQuestInfo.title, progress, duration)
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

    fun startQuest() {
        questHelper.setQuestRunning(basicQuestInfo.title, true)
        isQuestRunning = true
        timerActive = true

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Clear any existing data and set fresh start time
        prefs.edit()
            .putLong(KEY_START_TIME + questKey, System.currentTimeMillis())
            .putLong(KEY_PAUSED_ELAPSED + questKey, 0L)
            .apply()
    }

    // Load initial state
    LaunchedEffect(Unit) {
        if (isQuestRunning && !isQuestComplete) {
            timerActive = true
        }
    }

    // Handle the timer - use timerActive state to trigger/stop
    LaunchedEffect(timerActive) {
        if (timerActive) {
            // Get the start time from SharedPreferences or use current time if not found
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val startTimeKey = KEY_START_TIME + questKey
            val pausedElapsedKey = KEY_PAUSED_ELAPSED + questKey

            // Get saved values or use defaults
            val savedStartTime = prefs.getLong(startTimeKey, 0L)
            val pausedElapsed = prefs.getLong(pausedElapsedKey, 0L)

            val startTime = if (savedStartTime == 0L) {
                // First time starting the timer
                val newStartTime = System.currentTimeMillis() - pausedElapsed
                prefs.edit().putLong(startTimeKey, newStartTime).apply()
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
                    updateTimerNotification(context, basicQuestInfo.title, progress, duration)
                }

                // If completed, mark quest as done
                if (progress >= 1f) {
                    questHelper.markQuestAsComplete(basicQuestInfo.title, getCurrentDate())
                    questHelper.setQuestRunning(basicQuestInfo.title, false)
                    questHelper.updateQuestInfo<DeepFocus>(basicQuestInfo,{deepFocus})
                    isQuestComplete = true
                    isQuestRunning = false
                    timerActive = false

                    // Clear saved times
                    prefs.edit()
                        .remove(startTimeKey)
                        .remove(pausedElapsedKey)
                        .apply()

                    // Cancel notification when complete
                    cancelTimerNotification(context)
                }

                delay(1000) // Update every second instead of 100ms to reduce battery usage
            }
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
                    prefs.edit().putLong(pausedElapsedKey, elapsedTime).apply()
                }
            }
        }
    }

    // Prevent back navigation when quest is running
    BackHandler(isQuestRunning) {}

    // Convert progress state to MutableState<Float> for BaseQuestView
    val progressState = remember(progress) { mutableFloatStateOf(progress) }

    BaseQuestView(
        hideStartQuestBtn = isQuestComplete || isQuestRunning,
        progress = progressState,
        onQuestStarted = {
            // Start the quest immediately - this is called when button is pressed
            startQuest()
        }) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = basicQuestInfo.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                fontFamily = JetBrainsMonoFont,
                modifier = Modifier.padding(top = 40.dp)
            )

            Text(
                text = "Reward: ${basicQuestInfo.reward} coins",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
            )

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
                text = if (!isQuestComplete) "Duration: ${duration / 60_000}m" else "Next Duration: ${duration / 60_000}m",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                modifier = Modifier.padding(top = 32.dp)
            )

            val pm = context.packageManager
            val apps = deepFocus.unrestrictedApps.mapNotNull { packageName ->
                try {
                    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() to packageName
                } catch (e: PackageManager.NameNotFoundException) {
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
                                if (isQuestRunning && !isQuestComplete) {
                                    updateTimerNotification(context, basicQuestInfo.title, progress, duration)
                                }
                                val intent = pm.getLaunchIntentForPackage(packageName)
                                intent?.let { context.startActivity(it) }
                            }
                    )
                }
            }

            Text(
                text = "Instructions",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 32.dp, bottom = 4.dp)
            )
            Text(
                text = basicQuestInfo.instructions,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Use your app's icon here
        .setContentTitle("Focus Quest: $questTitle")
        .setContentText("Remaining time: $timeText")
        .setProgress(100, (progress * 100).toInt(), false)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOngoing(true) // Make it persistent
        .setContentIntent(pendingIntent)
        .setAutoCancel(false)
        .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
}

private fun cancelTimerNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(NOTIFICATION_ID)
}