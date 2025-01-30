package launcher.launcher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import launcher.launcher.models.AppInfo


// Cache the app list in SharedPreferences
fun cacheApps(context: Context, apps: List<AppInfo>) {
    val sharedPreferences = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Json.encodeToString(apps)
    editor.putString("apps", json)
    editor.apply()
}

// Retrieve the cached app list from SharedPreferences
fun getCachedApps(context: Context): List<AppInfo> {
    val sharedPreferences = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("apps", null)
    return if (json != null) {
        Json.decodeFromString(json)
    } else {
        emptyList()
    }
}

suspend fun reloadApps(
    packageManager: PackageManager,
    context: Context
): Result<List<AppInfo>> {
    return withContext(Dispatchers.IO) {
        try {
            // Fetch the latest app list from the PackageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val apps = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                .mapNotNull { resolveInfo ->
                    resolveInfo.activityInfo?.applicationInfo?.let { appInfo ->
                        AppInfo(
                            name = appInfo.loadLabel(packageManager).toString(),
                            packageName = appInfo.packageName
                        )
                    }
                }

            // Cache the app list in SharedPreferences
            cacheApps(context, apps)

            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}