package neth.iecal.questphone.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import neth.iecal.questphone.MainActivity
import neth.iecal.questphone.R
import neth.iecal.questphone.utils.getKeyboards
import neth.iecal.questphone.utils.reloadApps
import kotlin.collections.iterator


class AppBlockerService : Service() {

    private val TAG = "AppBockServiceFG"
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var handler: Handler
    private var lastForegroundPackage: String? = null


    // Default locked apps - will be overridden by saved preferences
    private val lockedApps = mutableSetOf<String>()


    companion object {
        const val NOTIFICATION_CHANNEL_ID = "AppBlockService"
        const val NOTIFICATION_ID = 123
        var isOverlayActive = false
        var currentLockedPackage: String? = null

        // Polling intervals
        private const val STANDARD_POLLING_INTERVAL_MS = 100L

    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AppBlockService onCreate")
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
        setupBroadcastListeners()
        loadLockedApps()
        if(ServiceInfo.deepFocus.isRunning) turnDeepFocus()
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoringApps()
        ServiceInfo.appBlockerService= this
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun setupBroadcastListeners(){
        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER)
            addAction(INTENT_ACTION_UNLOCK_APP)
            addAction(INTENT_ACTION_START_DEEP_FOCUS)
            addAction(INTENT_ACTION_STOP_DEEP_FOCUS)
        }

        Log.d("AppBlockerSrvieFg","registering reciever")
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(appMonitorRunnable)
        ServiceInfo.appBlockerService = null
        showHomwScreenOverlay()
        // remove the notification when service is destroyed
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)

        unregisterReceiver(refreshReceiver)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "App Block Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        // Add description to make the channel's purpose clear
        serviceChannel.description = "Allows the appblocker to be run"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        // Import required for NotificationCompat
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_info_24) // This requires a notification icon in your drawable resources
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        // Create a PendingIntent for when the notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)

        return builder.build()
    }

    private fun startMonitoringApps() {
        handler.post(appMonitorRunnable)
    }

    private val appMonitorRunnable = object : Runnable {
        override fun run() {
            detectAndHandleForegroundApp()
            handler.postDelayed(this, STANDARD_POLLING_INTERVAL_MS)
        }
    }

    private fun detectAndHandleForegroundApp() {
        val currentTime = System.currentTimeMillis()

        cleanUpExpiredUnlocks(currentTime)

        // Query events for a slightly longer period to catch transitions
        val usageEvents = usageStatsManager.queryEvents(currentTime - 2000, currentTime)
        val event = UsageEvents.Event()
        var detectedForegroundPackage: String?
        val recentLockedAppActivities = mutableSetOf<String>()

        // Process usage events to detect foreground app and recent locked app activities
        var latestTimestamp: Long = 0
        var currentForegroundAppFromEvents: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.timeStamp > latestTimestamp) {
                    latestTimestamp = event.timeStamp
                    currentForegroundAppFromEvents = event.packageName
                }
                if (lockedApps.contains(event.packageName)) {
                    recentLockedAppActivities.add(event.packageName)
                }
            }
        }
        detectedForegroundPackage = currentForegroundAppFromEvents ?: getCurrentForegroundApp()

        // If lock screen is active but pushed to background, bring it back
        if (isOverlayActive && detectedForegroundPackage != null && lockedApps.contains(
                detectedForegroundPackage
            )
        ) {
            // Check if the current foreground app is the one that should be locked
            if (currentLockedPackage == detectedForegroundPackage) {
                handler.post { refreshHomeScreenOverlay() }
                return
            } else {
                // If another locked app comes to foreground while overlay is active for a different app
                // or if an unlocked app comes to foreground..
            }
        }

        // Check if we're on home screen
        val isHomeScreen = isLauncherApp(detectedForegroundPackage)
        if (isHomeScreen) {
            handleHomeScreenDetected(detectedForegroundPackage)
            return
        }

        // Handle locked app detection
        if (shouldShowLockScreen(recentLockedAppActivities, detectedForegroundPackage)) {
            return // Lock screen shown, no further processing needed in this cycle
        }

        // Process foreground app state
        detectedForegroundPackage?.let { foregroundPackage ->
            processForegroundApp(foregroundPackage)
        }
    }

    // Cleans up apps whose temporary unlock duration has expired
    private fun cleanUpExpiredUnlocks(currentTime: Long) {
        val expiredApps = mutableListOf<String>()
        for ((packageName, expiryTime) in ServiceInfo.unlockedApps) {
            if (currentTime >= expiryTime) {
                expiredApps.add(packageName)
                Log.d(TAG, "Temporary unlock expired for: $packageName")
            }
        }
        expiredApps.forEach { ServiceInfo.unlockedApps.remove(it) }
    }

    private fun shouldShowLockScreen(
        recentLockedAppActivities: Set<String>,
        detectedForegroundPackage: String?
    ): Boolean {
        if (detectedForegroundPackage == null) return false

        // Skip showing lock screen for our own app
        if (detectedForegroundPackage == packageName) {
            return false
        }

        val isAppCurrentlyLocked = lockedApps.contains(detectedForegroundPackage)
        // Check if the app is currently temporarily unlocked (and not expired)
        val isTemporarilyUnlocked = ServiceInfo.unlockedApps.containsKey(detectedForegroundPackage)

        if (isAppCurrentlyLocked &&
            !isOverlayActive &&
            !isTemporarilyUnlocked // Make sure it's not temporarily unlocked
        ) {
            Log.d(TAG, "Lock condition met for: $detectedForegroundPackage (showing lock screen)")
            showLockScreenFor(detectedForegroundPackage)
            return true
        }
        return false
    }

    private fun showLockScreenFor(packageName: String) {
        currentLockedPackage = packageName
        isOverlayActive = true
        // Ensure biometric auth in progress is reset if we are showing a new lock screen
        handler.post { showHomwScreenOverlay() }
    }

    private fun handleHomeScreenDetected(detectedForegroundPackage: String?) {
        // If we came from a locked app that was temporarily unlocked, its timer continues.
        // We only clear the `currentLockedPackage` and `isOverlayActive` flags.
        if (currentLockedPackage != null) {
            Log.d(TAG, "User exited locked app, clearing current lock state flags.")
            currentLockedPackage = null
            isOverlayActive = false
        }
        lastForegroundPackage = detectedForegroundPackage
    }

    private fun processForegroundApp(foregroundPackage: String) {
        // Handle launcher app detection (already handled in detectAndHandleForegroundApp, but good for clarity)
        if (isLauncherApp(foregroundPackage)) {
            handleHomeScreenDetected(foregroundPackage)
            return
        }

        // NEW: Check if the app is currently temporarily unlocked (and not expired)
        val isCurrentlyTemporarilyUnlocked = ServiceInfo.unlockedApps.containsKey(foregroundPackage)

        // If the current foreground app is one that is temporarily unlocked, do nothing further.
        if (isCurrentlyTemporarilyUnlocked) {
            if (currentLockedPackage == foregroundPackage) { // Ensure consistency
                currentLockedPackage = null
            }
            lastForegroundPackage = foregroundPackage
            return
        }

        // If a new app (not the temporarily unlocked one) comes to the foreground,
        // and it's not due to biometric auth flow for the *same* app.
        // We now handle `temporarilyUnlockedAppsWithExpiry` as a map, so we don't clear a single flag.
        // The cleanup is handled by `cleanUpExpiredUnlocks`.

        // Check if the current foreground app needs to be locked
        // This is the main locking condition after other checks.
        if (lockedApps.contains(foregroundPackage) &&
            !isOverlayActive // Don't show if already showing
        ) {
            Log.d(
                TAG,
                "Locked app detected in processForegroundApp: $foregroundPackage (showing lock screen)"
            )
            showLockScreenFor(foregroundPackage)
        }

        lastForegroundPackage = foregroundPackage
    }

    private fun refreshHomeScreenOverlay() {
        if (isOverlayActive && currentLockedPackage != null) {
            Log.d(TAG, "Refreshing overlay for $currentLockedPackage")
            val currentIntent = Intent(this, MainActivity::class.java)
            currentIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            // Ensure the package name is passed, in case the overlay needs to re-verify
            currentIntent.putExtra("locked_package", currentLockedPackage)
            startActivity(currentIntent)
        }
    }

    private fun showHomwScreenOverlay() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )
        intent.putExtra("locked_package", currentLockedPackage)
        startActivity(intent)
    }

    // unlockApp to set an expiry time
    fun unlockApp(unlockedPackageName: String, duration: Int) {
        if (!isAppUnlocked(unlockedPackageName)) {
            val expiryTime = System.currentTimeMillis() + duration
            ServiceInfo.unlockedApps[unlockedPackageName] = expiryTime
            Log.d(TAG, "App unlocked via PIN: $unlockedPackageName. Unlocked until: $expiryTime")
            isOverlayActive = false
            if (currentLockedPackage == unlockedPackageName) {
                currentLockedPackage = null
            }
        }
    }

    fun isAppUnlocked(packageName:String):Boolean{
        return ServiceInfo.unlockedApps.containsKey(packageName)
    }

    fun isAppLocked(packageName: String): Boolean {
        return lockedApps.contains(packageName)
    }

    // Modified isAppTemporarilyUnlocked to check for expiry
    fun isAppTemporarilyUnlocked(packageName: String): Boolean {
        val expiryTime = ServiceInfo.unlockedApps[packageName]
        return expiryTime != null && System.currentTimeMillis() < expiryTime
    }

    fun loadLockedApps() {
        val sp = getSharedPreferences("distractions", MODE_PRIVATE)
        val apps = sp.getStringSet("distracting_apps", emptySet<String>())

        lockedApps.clear()
        if(apps?.isNotEmpty() == true){
            lockedApps.addAll(apps)
        }
        Log.d(TAG, "Loaded locked apps: $lockedApps")
    }

    fun addLockedApp(packageName: String) {
        lockedApps.add(packageName)
    }

    fun removeLockedApp(packageName: String) {
        lockedApps.remove(packageName)
        // If removing a locked app, also remove it from temporary unlock if it was there
        ServiceInfo.unlockedApps.remove(packageName)
    }


    fun getLockedApps(): Set<String> {
        return lockedApps.toSet() // Return a copy
    }

    private fun isLauncherApp(packageName: String?): Boolean {
        if (packageName == null) return false
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun getCurrentForegroundApp(): String? {
        var currentApp: String? = null
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val appList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 10,
            time
        )
        if (appList != null && appList.isNotEmpty()) {
            val sortedMap = sortedMapOf<Long, String>()
            for (usageStats in appList) {
                sortedMap[usageStats.lastTimeUsed] = usageStats.packageName
            }
            if (sortedMap.isNotEmpty()) {
                currentApp = sortedMap[sortedMap.lastKey()]
            }
        }
        return currentApp
    }

    private fun turnDeepFocus(){
        CoroutineScope(Dispatchers.IO).launch {
            val pm = applicationContext.packageManager
            val result = reloadApps(pm, applicationContext)

            if(result.isSuccess){
                var allApps = result.getOrDefault(emptyList())
                val keyboardApps = getKeyboards(applicationContext)

                allApps = allApps.filter {
                    !ServiceInfo.deepFocus.exceptionApps.contains(it.packageName) && !keyboardApps.contains(it.packageName) && it.packageName != "launcher.launcher"
                }

                lockedApps.clear()
                lockedApps.addAll(allApps.map { it.packageName })
                Log.d("AppBlockerServiceFg","Turning on FocusMode ${lockedApps.toString()}")

            }
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG,intent?.action.toString())
            if (intent == null) return
            when (intent.action) {
                INTENT_ACTION_REFRESH_APP_BLOCKER -> loadLockedApps()
                INTENT_ACTION_START_DEEP_FOCUS -> {
                    ServiceInfo.deepFocus.exceptionApps = intent.getStringArrayListExtra("exception")?.toHashSet()!!
                    ServiceInfo.deepFocus.isRunning = true
                    turnDeepFocus()
                }
                INTENT_ACTION_STOP_DEEP_FOCUS -> {
                    ServiceInfo.deepFocus.isRunning = false
                    ServiceInfo.deepFocus.exceptionApps = hashSetOf<String>()
                    loadLockedApps()
                }
                INTENT_ACTION_UNLOCK_APP -> {
                    val interval = intent.getIntExtra("selected_time", 0)
                    val coolPackage = intent.getStringExtra("package_name") ?: ""

                    Log.d("AppBlockerServiceFG", "Received cooldown broadcast for $coolPackage, duration: $interval ms")

                    // Only proceed if we have a valid package and duration
                    if (coolPackage.isNotEmpty() && interval > 0) {
                        unlockApp(coolPackage,interval)
                    } else {
                        Log.e("AppBlockerServiceFG", "Invalid cooldown parameters: package=$coolPackage, interval=$interval")
                    }
                }
            }
        }
    }
}