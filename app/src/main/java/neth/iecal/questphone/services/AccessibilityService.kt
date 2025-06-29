package neth.iecal.questphone.services

import android.R
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
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import neth.iecal.questphone.blockers.AppBlocker

private const val NOTIFICATION_CHANNEL_ID = "app_cooldown_channel"
private const val NOTIFICATION_ID = 1002

class AccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var timerRunnable: Runnable? = null
    private var lastPackage = ""
    val appBlocker = AppBlocker()
    private var isTimerRunning = false
    private var currentCooldownPackage = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        try {
            val eventPackageName = event.packageName?.toString()
            val texts by lazy { // Use lazy initialization to collect text only when needed
                val collected = mutableSetOf<String>()
                collectAllText(rootNode, collected)
                collected
            }
            val appName by lazy { getString(neth.iecal.questphone.R.string.app_name) }

            // --- Protection Logic ---
            val appNameFound by lazy { texts.any { it.equals(appName, ignoreCase = true) } }

            // Scenario 1: Uninstall confirmation dialog (package-agnostic)
            val uninstallDialogText = "Do you want to uninstall this app?"
            if (texts.any { it.equals(uninstallDialogText, ignoreCase = true) } && appNameFound) {
                Log.d("AccessibilityLog", "Uninstall confirmation dialog detected. Going back.")
                performGlobalAction(GLOBAL_ACTION_BACK)
                return // Exit early
            }

            // Scenario 2: System Settings screens
            if (eventPackageName == "com.android.settings") {
                // App Info page
                if (texts.any { it.equals("Uninstall", ignoreCase = true) } && appNameFound) {
                    Log.d("AccessibilityLog", "App Info screen detected. Going back.")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }
                // Accessibility settings page
                if (texts.any { it.equals("Accessibility", ignoreCase = true) } && appNameFound) {
                    Log.d("AccessibilityLog", "Accessibility settings screen detected. Going back.")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }
                // "Add a language" page
                if (texts.any { it.equals("Add a language", ignoreCase = true) }) {
                    Log.d("AccessibilityLog", "'Add a language' screen detected. Going back.")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }
            }

            // --- Original AppBlocker logic ---
            if (event.packageName == packageName) return
            val currentPackageName = event.packageName?.toString() ?: return
            if (lastPackage == currentPackageName) return
            lastPackage = currentPackageName
            Log.d("AppBlockerService", "Switched to app $currentPackageName")
            handleAppBlockerResult(appBlocker.doesAppNeedToBeBlocked(currentPackageName), currentPackageName)
            handleDeepFocusResult(ServiceInfo.deepFocus.doesAppNeedToBeBlocked(currentPackageName))
        } finally {
            // IMPORTANT: Always recycle the root node after we are done with it to prevent memory leaks.
            rootNode.recycle()
        }
    }

    private fun collectAllText(node: AccessibilityNodeInfo?, texts: MutableSet<String>) {
        if (node == null) return
        node.text?.let { texts.add(it.toString()) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            collectAllText(child, texts)
            child?.recycle()
        }
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
                    ServiceInfo.deepFocus.exceptionApps = intent.getStringArrayListExtra("exception")?.toHashSet()!!
                    ServiceInfo.deepFocus.isRunning = true
                }
                INTENT_ACTION_STOP_DEEP_FOCUS -> {
                    ServiceInfo.deepFocus.isRunning = false
                    ServiceInfo.deepFocus.exceptionApps = hashSetOf<String>()
                }
                INTENT_ACTION_UNLOCK_APP -> {
                    val interval = intent.getIntExtra("selected_time", 0).toLong()
                    val coolPackage = intent.getStringExtra("package_name") ?: ""
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
            addAction(INTENT_ACTION_UNLOCK_APP)
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
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("AppBlockerService", "Notification channel created: $NOTIFICATION_CHANNEL_ID")
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Failed to create notification channel: ${e.message}")
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateTimerNotification(packageName: String, progress: Float, remainingSeconds: Long) {
        try {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

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
                .setSmallIcon(R.drawable.ic_lock_idle_alarm)
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
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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