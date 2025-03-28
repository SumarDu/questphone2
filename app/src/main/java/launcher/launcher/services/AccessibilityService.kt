package launcher.launcher.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
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
import launcher.launcher.blockers.AppBlocker


/**
 * Refreshes information about blocked app list
 */
const val INTENT_ACTION_REFRESH_APP_BLOCKER = "launcher.launcher.refresh.appblocker"

/**
 * Add cooldown to an app.
 * This broadcast should always be sent together with the following keys:
 * selected_time: Int -> Duration of cooldown in minutes
 * result_id : String -> Package name of app to be put into cooldown
 */
const val INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN =
    "launcher.launcher.refresh.appblocker.cooldown"

class AccessibilityService() : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null



    private var lastPackage = ""

    val appBlocker = AppBlocker()
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        val packageName = event?.packageName.toString()
        if (lastPackage == packageName || packageName == getPackageName()) return


        lastPackage = packageName
        Log.d("AppBlockerService", "Switched to app $packageName")

        handleAppBlockerResult(appBlocker.doesAppNeedToBeBlocked(packageName), packageName)
    }

    private fun handleAppBlockerResult(result: AppBlocker.AppBlockerResult, packageName: String) {
        Log.d("AppBlockerService", "$packageName result : $result")

        if (result.cooldownEndTime != -1L) {
            setUpForcedRefreshChecker(packageName, result.cooldownEndTime)
        }

        if (!result.isBlocked) return


        pressHome()


    }

    private fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }


    /**
     * Setup a runnable that executes after n millis to check if a package is still being used that was allowed to be used previously
     * as it was put into cooldown or found in cheat-minutes. Basically shows the warning dialog after cooldown is over.
     * @param coolPackage
     * @param endMillis
     */
    private fun setUpForcedRefreshChecker(coolPackage: String, endMillis: Long) {
        if (updateRunnable != null) {
            updateRunnable?.let { handler.removeCallbacks(it) }
            updateRunnable = null
        }
        updateRunnable = Runnable {

            Log.d("AppBlockerService", "Triggered Recheck for  $coolPackage")
            try {
                if (rootInActiveWindow.packageName == coolPackage) {
                    handleAppBlockerResult(
                        AppBlocker.AppBlockerResult(true),
                        coolPackage
                    )
                    lastPackage = ""
                    appBlocker.removeCooldownFrom(coolPackage)
                }
            } catch (e: Exception) {
                Log.e("AppBlockerService", e.toString())
                setUpForcedRefreshChecker(coolPackage, endMillis + 60_000) // recheck after a minute
            }
        }

        handler.postAtTime(updateRunnable!!, endMillis)
    }
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                INTENT_ACTION_REFRESH_APP_BLOCKER -> setupAppBlocker()
                INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN -> {
                    val interval =
                        intent.getIntExtra("selected_time", 0)
                    val coolPackage = intent.getStringExtra("result_id") ?: ""
                    val cooldownUntil =
                        SystemClock.uptimeMillis() + interval
                    appBlocker.putCooldownTo(
                        coolPackage,
                        cooldownUntil
                    )
                    setUpForcedRefreshChecker(coolPackage, cooldownUntil)
                }
            }

        }

    }

    private fun setupAppBlocker() {
        val sp = getSharedPreferences("distractions", MODE_PRIVATE)
        appBlocker.blockedAppsList = sp.getStringSet("distracting_apps",emptySet<String>()) as HashSet<String>
    }
    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        setupAppBlocker()

        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER)
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
    }
}