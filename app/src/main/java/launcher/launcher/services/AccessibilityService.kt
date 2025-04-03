package launcher.launcher.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import launcher.launcher.blockers.AppBlocker
import launcher.launcher.blockers.DeepFocus

const val INTENT_ACTION_REFRESH_APP_BLOCKER = "launcher.launcher.refresh.appblocker"
const val INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN = "launcher.launcher.refresh.appblocker.cooldown"
const val INTENT_ACTION_START_DEEP_FOCUS = "launcher.launcher.start.deepfocus"
const val INTENT_ACTION_STOP_DEEP_FOCUS = "launcher.launcher.stop.deepfocus"
private const val NOTIFICATION_CHANNEL_ID = "app_cooldown_channel"
private const val NOTIFICATION_ID = 1002

class AccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var timerRunnable: Runnable? = null
    private var lastPackage = ""
    val appBlocker = AppBlocker()
    val deepFocus = DeepFocus()
    private var isTimerRunning = false
    private var currentCooldownPackage = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Only process TYPE_WINDOW_STATE_CHANGED events to detect app switches
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||event.packageName == packageName) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Avoid processing the same package multiple times
        if (lastPackage == packageName) return

        lastPackage = packageName
        Log.d("AppBlockerService", "Switched to app $packageName")
        handleAppBlockerResult(appBlocker.doesAppNeedToBeBlocked(packageName), packageName)
        handleDeepFocusResult(deepFocus.doesAppNeedToBeBlocked(packageName))
    }

    private fun handleDeepFocusResult(isBlocked: Boolean) {
        if(isBlocked){
            pressHome()
        }
    }

    private fun handleAppBlockerResult(result: AppBlocker.AppBlockerResult, packageName: String) {
        Log.d("AppBlockerService", "$packageName result: $result")

        if (result.cooldownEndTime != -1L) {
            val remainingTime = result.cooldownEndTime - SystemClock.uptimeMillis()
            Log.d("AppBlockerService", "Cooldown detected for $packageName, remaining: $remainingTime ms")
            setUpForcedRefreshChecker(packageName, result.cooldownEndTime)
        }

        if (!result.isBlocked) {
            return
        }
        pressHome()
    }

    private fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun setUpForcedRefreshChecker(coolPackage: String, endMillis: Long) {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = Runnable {
            Log.d("AppBlockerService", "Triggered Recheck for $coolPackage")
            try {
                if (rootInActiveWindow?.packageName == coolPackage) {
                    handleAppBlockerResult(AppBlocker.AppBlockerResult(true), coolPackage)
                    lastPackage = ""
                    appBlocker.removeCooldownFrom(coolPackage)
                    // Also stop the timer notification when forcing recheck
                    if (currentCooldownPackage == coolPackage) {
                        stopCooldownTimer()
                    }
                }
            } catch (e: Exception) {
                Log.e("AppBlockerService", "Recheck error: ${e.message}")
                setUpForcedRefreshChecker(coolPackage, endMillis + 60_000)
            }
        }
        handler.postAtTime(updateRunnable!!, endMillis)
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                INTENT_ACTION_REFRESH_APP_BLOCKER -> setupAppBlocker()
                INTENT_ACTION_START_DEEP_FOCUS -> {
                    deepFocus.exceptionApps = intent.getStringArrayListExtra("exception")?.toHashSet()!!
                    deepFocus.isRunning = true
                }
                INTENT_ACTION_STOP_DEEP_FOCUS -> {
                    deepFocus.isRunning = false
                    deepFocus.exceptionApps = hashSetOf<String>()
                }
                INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN -> {
                    val interval = intent.getIntExtra("selected_time", 0).toLong()
                    val coolPackage = intent.getStringExtra("result_id") ?: ""
                    val cooldownUntil = SystemClock.uptimeMillis() + interval

                    Log.d("AppBlockerService", "Received cooldown broadcast for $coolPackage, duration: $interval ms")

                    // Only proceed if we have a valid package and duration
                    if (coolPackage.isNotEmpty() && interval > 0) {
                        appBlocker.putCooldownTo(coolPackage, cooldownUntil)
                        setUpForcedRefreshChecker(coolPackage, cooldownUntil)
                        startCooldownTimer(coolPackage, interval)
                    } else {
                        Log.e("AppBlockerService", "Invalid cooldown parameters: package=$coolPackage, interval=$interval")
                    }
                }
            }
        }
    }

    private fun setupAppBlocker() {
        val sp = getSharedPreferences("distractions", MODE_PRIVATE)
        appBlocker.blockedAppsList = sp.getStringSet("distracting_apps", emptySet<String>()) as HashSet<String>
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AppBlockerService", "Service connected")
        setupAppBlocker()
        createNotificationChannel() // Create channel at service start

        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER)
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN)
            addAction(INTENT_ACTION_START_DEEP_FOCUS)
            addAction(INTENT_ACTION_STOP_DEEP_FOCUS)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(refreshReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Failed to register receiver: ${e.message}")
        }
    }

    private fun startCooldownTimer(packageName: String, durationMs: Long) {
        // Stop any existing timer first
        stopCooldownTimer()

        currentCooldownPackage = packageName
        Log.d("AppBlockerService", "Starting cooldown timer for $packageName, duration: $durationMs ms")

        val startTime = SystemClock.uptimeMillis()
        val endTime = startTime + durationMs
        val totalSeconds = durationMs / 1000

        // Show initial notification immediately
        updateTimerNotification(packageName, 0f, totalSeconds)

        isTimerRunning = true
        timerRunnable = object : Runnable {
            override fun run() {
                val currentTime = SystemClock.uptimeMillis()
                val elapsedMs = currentTime - startTime
                val remainingMs = endTime - currentTime

                // Convert to seconds for display and calculations
                val remainingSeconds = remainingMs / 1000

                val progress = elapsedMs.toFloat() / durationMs.toFloat()

                if (remainingSeconds > 0) {
                    Log.d("AppBlockerService", "Updating notification: $packageName, progress: ${progress * 100}%, remaining: $remainingSeconds s")
                    updateTimerNotification(packageName, progress, remainingSeconds)
                    handler.postDelayed(this, 1000)
                } else {
                    Log.d("AppBlockerService", "Cooldown timer completed for $packageName")

                    // Reset cooldown in the app blocker when timer finishes
                    appBlocker.removeCooldownFrom(packageName)

                    // Final notification update showing completion
                    updateTimerNotification(packageName, 1f, 0)

                    // Small delay before removing the notification
                    handler.postDelayed({
                        stopCooldownTimer()
                    }, 2000)
                }
            }
        }

        // Start the timer runnable
        handler.post(timerRunnable!!)
    }

    private fun stopCooldownTimer() {
        timerRunnable?.let {
            handler.removeCallbacks(it)
            Log.d("AppBlockerService", "Stopped cooldown timer")
        }
        isTimerRunning = false
        timerRunnable = null
        currentCooldownPackage = ""
        cancelTimerNotification()
    }

    private fun createNotificationChannel() {
        val name = "App Cooldown"
        val descriptionText = "Shows remaining cooldown time for apps"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(true)
            enableVibration(false)
            enableLights(false)
        }

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("AppBlockerService", "Notification channel created: $NOTIFICATION_CHANNEL_ID")
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Failed to create notification channel: ${e.message}")
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateTimerNotification(packageName: String, progress: Float, remainingSeconds: Long) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Check if notifications are enabled
            val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (channel != null) {
                if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                    Log.w("AppBlockerService", "Notification channel is disabled")
                    return
                }
            }

            // Create a basic intent for the app
            val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_HOME)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Format time display
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            val timeText = String.format("%d:%02d", minutes, seconds)

            // Get app name for better UX
            val appName = try {
                packageManager.getApplicationInfo(packageName, 0)
                    .loadLabel(packageManager).toString()
            } catch (e: Exception) {
                Log.w("AppBlockerService", "Failed to get app name: ${e.message}")
                packageName
            }

            // Build the notification
            val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("App Cooldown: $appName")
                .setContentText("Time remaining: $timeText")
                .setProgress(100, (progress * 100).toInt(), false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setSilent(true)

            // Set foreground if device is on Android 8.0 or higher
            builder.setChannelId(NOTIFICATION_CHANNEL_ID)

            val notification = builder.build()

            // Post the notification
            Log.d("AppBlockerService", "Posting notification for $packageName with time $timeText")
            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e("AppBlockerService", "Failed to update notification: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun cancelTimerNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d("AppBlockerService", "Notification cancelled")
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Failed to cancel notification: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable?.let { handler.removeCallbacks(it) }
        try {
            unregisterReceiver(refreshReceiver)
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Failed to unregister receiver: ${e.message}")
        }
        stopCooldownTimer()
        Log.d("AppBlockerService", "Service destroyed")
    }
}