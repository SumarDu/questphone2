package neth.iecal.questphone.blockers


import android.os.SystemClock
import android.util.Log
import neth.iecal.questphone.services.ServiceInfo

class AppBlocker {

    var blockedAppsList = hashSetOf("")

    /**
     * Check if app needs to be blocked
     *
     * @param packageName
     * @return
     */
    fun doesAppNeedToBeBlocked(packageName: String): AppBlockerResult {

        if(ServiceInfo.unlockedApps.containsKey(packageName)){
            // check if app has surpassed the cooldown period
            if (ServiceInfo.unlockedApps[packageName]!! < SystemClock.uptimeMillis()){
                removeCooldownFrom(packageName)
                return AppBlockerResult(isBlocked = true)
            }

            // app is still under cooldown
            return AppBlockerResult(
                isBlocked = false,
                cooldownEndTime = ServiceInfo.unlockedApps[packageName]!!
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
        ServiceInfo.unlockedApps[packageName] = endTime
        Log.d("cooldownAppsList", ServiceInfo.unlockedApps.toString())
    }

    fun removeCooldownFrom(packageName: String) {
        ServiceInfo.unlockedApps.remove(packageName)
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