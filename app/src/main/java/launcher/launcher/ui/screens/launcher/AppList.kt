package launcher.launcher.ui.screens.launcher

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

// Group apps by first letter
data class AppGroup(val letter: Char, val apps: List<AppInfo>)

@Composable
fun AppList(onNavigateToQuestTracker: () -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    // State management
    val appsState = remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val groupedAppsState = remember { mutableStateOf<List<AppGroup>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val showCoinDialog = remember { mutableStateOf(false) }
    val selectedPackage = remember { mutableStateOf("") }

    val coinHelper = CoinHelper(context)

    // Load and group apps
    LaunchedEffect(Unit) {
        loadInitialApps(appsState, isLoading, context)
        withContext(Dispatchers.IO) {
            reloadApps(packageManager, context)
                .onSuccess { apps ->
                    appsState.value = apps
                    groupedAppsState.value = groupAppsByLetter(apps)
                    isLoading.value = false
                }
                .onFailure { error ->
                    errorState.value = error.message
                    isLoading.value = false
                }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        AppListContent(
            groupedApps = groupedAppsState.value,
            isLoading = isLoading.value,
            error = errorState.value,
            innerPadding = innerPadding,
            onAppClick = { packageName ->
                showCoinDialog.value = true
                selectedPackage.value = packageName
            }
        )

        if (showCoinDialog.value) {
            CoinDialog(
                coins = coinHelper.getCoinCount(),
                onDismiss = { showCoinDialog.value = false },
                onConfirm = {
                    coinHelper.decrementCoinCount(1)
                    launchApp(context, selectedPackage.value)
                    showCoinDialog.value = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListContent(
    groupedApps: List<AppGroup>,
    isLoading: Boolean,
    error: String?,
    innerPadding: PaddingValues,
    onAppClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        when {
            isLoading -> {
                Text(
                    text = "Loading apps...",
                    modifier = Modifier.padding(16.dp)
                )
            }
            error != null -> {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                LazyColumn {
                    groupedApps.forEach { group ->
                        stickyHeader {
                            Text(
                                text = group.letter.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                            )
                        }
                        items(group.apps) { app ->
                            AppItem(
                                name = app.name,
                                packageName = app.packageName,
                                onAppPressed = onAppClick
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun groupAppsByLetter(apps: List<AppInfo>): List<AppGroup> {
    return apps
        .sortedBy { it.name }
        .groupBy { it.name.first().uppercaseChar() }
        .map { (letter, apps) -> AppGroup(letter, apps) }
}

private suspend fun loadInitialApps(
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

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let { context.startActivity(it) }
}