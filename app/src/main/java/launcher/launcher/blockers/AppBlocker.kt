package launcher.launcher.blockers


import android.os.SystemClock
import android.util.Log
import java.util.Calendar

class AppBlocker {

    // package-name -> end-time-in-millis
    private var cooldownAppsList:MutableMap<String,Long> = mutableMapOf()


    var blockedAppsList = hashSetOf("")

    /**
     * Check if app needs to be blocked
     *
     * @param packageName
     * @return
     */
    fun doesAppNeedToBeBlocked(packageName: String): AppBlockerResult {

        if(cooldownAppsList.containsKey(packageName)){
            // check if app has surpassed the cooldown period
            if (cooldownAppsList[packageName]!! < SystemClock.uptimeMillis()){
                removeCooldownFrom(packageName)
                return AppBlockerResult(isBlocked = true)
            }

            // app is still under cooldown
            return AppBlockerResult(
                isBlocked = false,
                cooldownEndTime = cooldownAppsList[packageName]!!
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
        cooldownAppsList[packageName] = endTime
        Log.d("cooldownAppsList",cooldownAppsList.toString())
    }

    fun removeCooldownFrom(packageName: String) {
        cooldownAppsList.remove(packageName)
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