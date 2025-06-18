package launcher.launcher.blockers


import android.os.SystemClock
import android.util.Log
import launcher.launcher.services.ServiceInfo.unlockedApps

class AppBlocker {

    var blockedAppsList = hashSetOf("")

    /**
     * Check if app needs to be blocked
     *
     * @param packageName
     * @return
     */
    fun doesAppNeedToBeBlocked(packageName: String): AppBlockerResult {

        if(unlockedApps.containsKey(packageName)){
            // check if app has surpassed the cooldown period
            if (unlockedApps[packageName]!! < SystemClock.uptimeMillis()){
                removeCooldownFrom(packageName)
                return AppBlockerResult(isBlocked = true)
            }

            // app is still under cooldown
            return AppBlockerResult(
                isBlocked = false,
                cooldownEndTime = unlockedApps[packageName]!!
            )
        }

        if (blockedAppsList.contains(packageName)) {
            return AppBlockerResult(
                isBlocked = true
            )
        }
        return AppBlockerResult(isBlocked = false)
    }
    fun putCooldownTo(packageName: String, endTime: Long) {
        unlockedApps[packageName] = endTime
        Log.d("cooldownAppsList",unlockedApps.toString())
    }

    fun removeCooldownFrom(packageName: String) {
        unlockedApps.remove(packageName)
    }

    /**
     * App blocker check result
     *
     * @property isBlocked
     * @property cooldownEndTime specifies when cooldown ends. returns -1 if not in cooldown
     */
    data class AppBlockerResult(
        val isBlocked: Boolean,
        val cooldownEndTime: Long = -1L
    )

}