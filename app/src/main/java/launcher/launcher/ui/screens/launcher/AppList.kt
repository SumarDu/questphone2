package launcher.launcher.ui.screens.launcher

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import launcher.launcher.data.AppInfo
import launcher.launcher.services.INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN
import launcher.launcher.ui.screens.launcher.components.AppItem
import launcher.launcher.ui.screens.launcher.components.CoinDialog
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.getCachedApps
import launcher.launcher.utils.reloadApps
import androidx.core.content.edit
import launcher.launcher.ui.screens.launcher.components.LowCoinsDialog

data class AppGroup(val letter: Char, val apps: List<AppInfo>)
private const val NOTIFICATION_CHANNEL_ID = "app_cooldown_channel"
private const val NOTIFICATION_ID = 1002

@Composable
fun AppList(onNavigateToQuestTracker: () -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val appsState = remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val groupedAppsState = remember { mutableStateOf<List<AppGroup>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val showCoinDialog = remember { mutableStateOf(false) }
    val selectedPackage = remember { mutableStateOf("") }
    val coinHelper = CoinHelper(context)

    val sp = context.getSharedPreferences("distractions", Context.MODE_PRIVATE)
    val lifecycleOwner = LocalLifecycleOwner.current

    var distractions = emptySet<String>()
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            distractions = sp.getStringSet("distracting_apps", emptySet<String>()) ?: emptySet()
        }
    }

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
        AppListWithScrollbar(
            groupedApps = groupedAppsState.value,
            isLoading = isLoading.value,
            error = errorState.value,
            innerPadding = innerPadding,
            onAppClick = { packageName ->
                if (distractions.contains(packageName)) {
                    val cooldownUntil = sp.getLong(packageName+"_time",-1L)

                    if (cooldownUntil==-1L || System.currentTimeMillis() > cooldownUntil) {
                        // Not under cooldown - show dialog
                        showCoinDialog.value = true
                        selectedPackage.value = packageName
                        sp.edit { remove(packageName + "_time") }
                    } else {
                        launchApp(context, packageName)
                    }
                } else {
                    // Not a distraction - launch directly
                    launchApp(context, packageName)
                }
            }
        )

        if (showCoinDialog.value) {
            if(coinHelper.canUserAffordPurchase(5)) {
                CoinDialog(
                    coins = coinHelper.getCoinCount(),
                    onDismiss = { showCoinDialog.value = false },
                    onConfirm = {
                        // Set cooldown time (10 minutes = 10 * 60 * 1000 milliseconds)
                        val cooldownTime = 10 * 60_000
                        sp.edit {
                            putLong(
                                selectedPackage.value + "_time",
                                System.currentTimeMillis() + cooldownTime
                            )
                        }
                        val intent = Intent().apply {
                            action = INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN
                            putExtra("selected_time", cooldownTime)
                            putExtra("result_id", selectedPackage.value)
                        }
                        context.sendBroadcast(intent)

                        coinHelper.decrementCoinCount(5)
                        launchApp(context, selectedPackage.value)
                        showCoinDialog.value = false
                    },
                    appName = try {
                        packageManager.getApplicationInfo(selectedPackage.value, 0)
                            .loadLabel(packageManager).toString()
                    } catch (_: Exception) {
                        selectedPackage.value
                    }
                )
            } else {
                LowCoinsDialog(
                    coins = coinHelper.getCoinCount(),
                    onDismiss = { showCoinDialog.value = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListWithScrollbar(
    groupedApps: List<AppGroup>,
    isLoading: Boolean,
    error: String?,
    innerPadding: PaddingValues,
    onAppClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // Map to store the starting index of each letter group
    val groupPositions = remember(groupedApps) {
        mutableMapOf<Char, Int>().apply {
            var currentIndex = 0
            groupedApps.forEach { group ->
                this[group.letter] = currentIndex
                currentIndex += 1 + group.apps.size // 1 for header + number of apps
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // Main app list
        Column(
            modifier = Modifier
                .weight(1f)
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
                    LazyColumn(state = listState) {
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

        // Minimal scrollbar
        if (!isLoading && error == null && groupedApps.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                groupedApps.forEach { group ->
                    Text(
                        text = group.letter.toString(),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val targetIndex = groupPositions[group.letter] ?: 0
                                    listState.animateScrollToItem(targetIndex)
                                }
                            }
                    )
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