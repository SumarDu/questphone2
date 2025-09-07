package neth.iecal.questphone.ui.screens.launcher

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.AppInfo
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.useCoins
import neth.iecal.questphone.services.AppBlockerService
import neth.iecal.questphone.services.INTENT_ACTION_UNLOCK_APP
import neth.iecal.questphone.services.ServiceInfo
import neth.iecal.questphone.ui.screens.launcher.components.AppItem
import neth.iecal.questphone.ui.screens.launcher.components.CoinDialog
import neth.iecal.questphone.ui.screens.launcher.components.LowCoinsDialog
import neth.iecal.questphone.utils.reloadApps

data class AppGroup(val letter: Char, val apps: List<AppInfo>)

@Composable
fun AppList(navController: NavController) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val appsState = remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val groupedAppsState = remember { mutableStateOf<List<AppGroup>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val showCoinDialog = remember { mutableStateOf(false) }
    val selectedPackage = remember { mutableStateOf("") }

    val sp = context.getSharedPreferences("distractions", Context.MODE_PRIVATE)
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var distractions = remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            distractions.value = sp.getStringSet("distracting_apps", emptySet<String>()) ?: emptySet()
        }
    }

    LaunchedEffect(Unit) {
        loadInitialApps(appsState, isLoading, context)
        withContext(Dispatchers.Default) {
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
                if (distractions.value.contains(packageName)) {
                    val cooldownUntil = ServiceInfo.unlockedApps[packageName] ?: 0L
                    if (cooldownUntil == -1L || System.currentTimeMillis() > cooldownUntil) {
                        showCoinDialog.value = true
                        selectedPackage.value = packageName
                    } else {
                        launchApp(context, packageName)
                    }
                } else {
                    launchApp(context, packageName)
                }
            }
        )

        if (showCoinDialog.value) {
            if (User.userInfo.coins >= 10) {
                CoinDialog(
                    coins = User.userInfo.coins,
                    onDismiss = { showCoinDialog.value = false },
                    onConfirm = {
                        scope.launch {
                            User.useCoins(10)
                            val intent = Intent(INTENT_ACTION_UNLOCK_APP).apply {
                                putExtra("package", selectedPackage.value)
                            }
                            startForegroundService(context, intent)
                            showCoinDialog.value = false
                            launchApp(context, selectedPackage.value)
                        }
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
                    coins = User.userInfo.coins,
                    onDismiss = { showCoinDialog.value = false },
                    appName = try {
                        packageManager.getApplicationInfo(selectedPackage.value, 0)
                            .loadLabel(packageManager).toString()
                    } catch (_: Exception) {
                        selectedPackage.value
                    },
                    navController = navController
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
                    LazyColumn(
                        state = listState
                    ) {
                        groupedApps.forEach { group ->
                            stickyHeader(key = "header_${group.letter}") {
                                Text(
                                    text = group.letter.toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(16.dp)
                                )
                            }
                            items(
                                items = group.apps,
                                key = { app -> "${group.letter}_${app.packageName}" }
                            ) { app ->
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
    withContext(Dispatchers.Default) {
        val cachedApps = reloadApps(context.packageManager, context).getOrNull() ?: emptyList()
    if (cachedApps.isNotEmpty()) {
        appsState.value = cachedApps
        isLoading.value = false
        }
    }
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let { context.startActivity(it) }
}