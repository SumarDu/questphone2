package neth.iecal.questphone.services

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import neth.iecal.questphone.blockers.DeepFocus
import neth.iecal.questphone.utils.json


const val INTENT_ACTION_REFRESH_APP_BLOCKER = "launcher.launcher.refresh.appblocker"
const val INTENT_ACTION_UNLOCK_APP = "launcher.launcher.refresh.appblocker.cooldown"
const val INTENT_ACTION_START_DEEP_FOCUS = "launcher.launcher.start.deepfocus"
const val INTENT_ACTION_STOP_DEEP_FOCUS = "launcher.launcher.stop.deepfocus"

object ServiceInfo{
    var appBlockerService: AppBlockerService? = null
    var isUsingAccessibilityService = false
    val deepFocus = DeepFocus()

    // Store the unlock time for each app that is temporarily unlocked
    val unlockedApps = mutableMapOf<String, Long>()

}

fun saveServiceInfo(context: Context) {
    val sp = context.getSharedPreferences("service_info", Context.MODE_PRIVATE)
    val unlockedAppsJson = json.encodeToString(ServiceInfo.unlockedApps)
    sp.edit().putString("unlocked_apps", unlockedAppsJson).putBoolean("is_using_accessibility", ServiceInfo.isUsingAccessibilityService).apply()
}

fun reloadServiceInfo(context: Context){
    val sp = context.getSharedPreferences("service_info", Context.MODE_PRIVATE)
    ServiceInfo.isUsingAccessibilityService = sp.getBoolean("is_using_accessibility",false)
    val unlockedAppsJson = sp.getString("unlocked_apps", null)
    if (unlockedAppsJson != null) {
        try {
            val unlockedMap = json.decodeFromString<Map<String, Long>>(unlockedAppsJson)
            ServiceInfo.unlockedApps.clear()
            ServiceInfo.unlockedApps.putAll(unlockedMap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}