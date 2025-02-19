package launcher.launcher.ui.screens.launcher


import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import launcher.launcher.models.AppInfo
import launcher.launcher.ui.screens.launcher.components.AppItem
import launcher.launcher.ui.screens.launcher.components.CoinDialog
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.getCachedApps
import launcher.launcher.utils.reloadApps

@Composable
fun AppList(onNavigateToQuestTracker: () -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val appsState = remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val isShowingLoadingStatus = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val isUserTryingOpenApp = remember { mutableStateOf(false) } // user is trying to open an app from the list
    val userSelectedPackage = remember { mutableStateOf("") }


    val coinHelper = CoinHelper(LocalContext.current)

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LaunchedEffect(Unit) {
            setFromCachedApps(appsState, isShowingLoadingStatus, context)
            withContext(Dispatchers.IO) {
                val result = reloadApps(packageManager, context)
                result.onSuccess { apps ->
                    appsState.value = apps
                    isShowingLoadingStatus.value = false
                }.onFailure { error ->
                    errorState.value = error.message
                    isShowingLoadingStatus.value = false
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()
            .padding(innerPadding)) {
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
                    AppItem(app.name, app.packageName, onAppPressed = { packageName ->
                        isUserTryingOpenApp.value = true
                        userSelectedPackage.value = packageName
                    })
                }
            }
            if(isUserTryingOpenApp.value){
                CoinDialog(coins = coinHelper.getCoinCount(), onDismiss = {
                    isUserTryingOpenApp.value = false
                }, onConfirm = {
                    coinHelper.decrementCoinCount(1)
                    val intent = context.packageManager.getLaunchIntentForPackage(userSelectedPackage.value)
                    context.startActivity(intent)
                })
            }

        }
    }
}

private fun setFromCachedApps(
    appsState: MutableState<List<AppInfo>>,
    isLoading: MutableState<Boolean>,
    context: Context
) {
    val cachedApps = getCachedApps(context)
    if (cachedApps.isNotEmpty()) {
        appsState.value = cachedApps
        isLoading.value = false
    }
}