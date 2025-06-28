package neth.iecal.questphone.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import neth.iecal.questphone.data.AppInfo

// In-memory cache
private var cachedAppList: List<AppInfo>? = null
private var lastCacheTime: Long = 0
private const val CACHE_DURATION = 5 * 60 * 1000 // 5 minutes

// Cache the app list in SharedPreferences
private suspend fun cacheApps(context: Context, apps: List<AppInfo>) = withContext(Dispatchers.IO) {
    val sharedPreferences = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Json.encodeToString(apps)
    editor.putString("apps", json)
    editor.apply()
    
    // Update memory cache
    cachedAppList = apps
    lastCacheTime = System.currentTimeMillis()
}

// Retrieve the cached app list from SharedPreferences
private suspend fun getCachedApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    // Check memory cache first
    if (cachedAppList != null && System.currentTimeMillis() - lastCacheTime < CACHE_DURATION) {
        return@withContext cachedAppList!!
    }

    val sharedPreferences = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("apps", null)
    return@withContext if (json != null) {
        Json.decodeFromString<List<AppInfo>>(json).also {
            cachedAppList = it
            lastCacheTime = System.currentTimeMillis()
        }
    } else {
        emptyList()
    }
}

suspend fun reloadApps(
    packageManager: PackageManager,
    context: Context,
    forceReload: Boolean = false
): Result<List<AppInfo>> {
    // Check memory cache first if not forcing reload
    if (!forceReload && cachedAppList != null && System.currentTimeMillis() - lastCacheTime < CACHE_DURATION) {
        return Result.success(cachedAppList!!)
    }

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
                .sortedBy { it.name } // Pre-sort the list

            // Cache the app list
            cacheApps(context, apps)

            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

suspend fun getBackgroundSystemApps(context: Context): List<ApplicationInfo> {
    return withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launchableApps = pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.applicationInfo.packageName }
            .toSet()

        allApps.filter { app ->
            (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && // system app
                    app.packageName !in launchableApps                  // not launchable
        }
    }
}

suspend fun getKeyboards(context: Context): List<String> = withContext(Dispatchers.IO) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabledMethods = imm.enabledInputMethodList
    enabledMethods.map { it.packageName }
}