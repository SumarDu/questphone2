package launcher.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import launcher.launcher.ui.theme.LauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LauncherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppList(
                        modifier = Modifier.padding(innerPadding)
                    )
                }

            }
        }
    }
}

@Composable
fun AppList(modifier: Modifier) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val appsState = remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val isShowingLoadingStatus = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf<String?>(null) }


    LaunchedEffect(Unit) {
        Log.d("setting cache","setting cache")
        setFromCachedApps(appsState,isShowingLoadingStatus,context)
        Log.d("setting cache","cache set")
        withContext(Dispatchers.IO){
            reloadApps(packageManager, appsState, isShowingLoadingStatus, errorState, context)

            Log.d("everything reloaded","cache set")
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        if (isShowingLoadingStatus.value) {
            Text(text = "Loading apps...", modifier = Modifier.padding(16.dp))
        } else if (errorState.value != null) {
            Text(
                text = "Error: ${errorState.value}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn {
            items(appsState.value) { app ->
                AppItem(app.name, app.packageName)
            }
        }

    }
}

@Composable
fun AppItem(name: String, packageName: String) {
    val context = LocalContext.current
    Text(
        text = name,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .clickable {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                context.startActivity(intent)
            }
    )
}

@Serializable
data class AppInfo(val name: String, val packageName: String)

private fun setFromCachedApps(
    appsState: MutableState<List<AppInfo>>,
    isLoading: MutableState<Boolean>,
    context: Context){

    val cachedApps = getCachedApps(context)
    if (cachedApps.isNotEmpty()) {
        appsState.value = cachedApps
        isLoading.value = false
    }
}

private fun reloadApps(
    packageManager: PackageManager,
    appsState: MutableState<List<AppInfo>>,
    isLoading: MutableState<Boolean>,
    errorState: MutableState<String?>,
    context: Context
) {
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

    appsState.value = apps
    isLoading.value = false
    errorState.value = null
}

// Cache the app list in SharedPreferences
private fun cacheApps(context: Context, apps: List<AppInfo>) {
    val sharedPreferences = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Json.encodeToString(apps)
    editor.putString("apps", json)
    editor.apply()
}

// Retrieve the cached app list from SharedPreferences
private fun getCachedApps(context: Context): List<AppInfo> {
    val sharedPreferences = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("apps", null)
    return if (json != null) {
        Json.decodeFromString(json)
    } else {
        emptyList()
    }
}