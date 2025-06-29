package neth.iecal.questphone.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.local.AppDatabase
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import neth.iecal.questphone.data.preferences.GestureSettingsRepository
import neth.iecal.questphone.ui.screens.launcher.AppInfo
import neth.iecal.questphone.ui.screens.launcher.AppsViewModel
import neth.iecal.questphone.ui.screens.launcher.AppsViewModelFactory
import neth.iecal.questphone.ui.screens.launcher.ListItem
import androidx.compose.foundation.Image

class GestureSettingsViewModel(private val repository: GestureSettingsRepository) : ViewModel() {
    val swipeUpApp = repository.swipeUpApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val swipeDownApp = repository.swipeDownApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val swipeLeftApp = repository.swipeLeftApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val swipeRightApp = repository.swipeRightApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSwipeUpApp(packageName: String) = viewModelScope.launch { repository.saveSwipeUpApp(packageName) }
    fun setSwipeDownApp(packageName: String) = viewModelScope.launch { repository.saveSwipeDownApp(packageName) }
    fun setSwipeLeftApp(packageName: String) = viewModelScope.launch { repository.saveSwipeLeftApp(packageName) }
    fun setSwipeRightApp(packageName: String) = viewModelScope.launch { repository.saveSwipeRightApp(packageName) }

    fun clearSwipeUpApp() = viewModelScope.launch { repository.saveSwipeUpApp("") }
    fun clearSwipeDownApp() = viewModelScope.launch { repository.saveSwipeDownApp("") }
    fun clearSwipeLeftApp() = viewModelScope.launch { repository.saveSwipeLeftApp("") }
    fun clearSwipeRightApp() = viewModelScope.launch { repository.saveSwipeRightApp("") }
}

class GestureSettingsViewModelFactory(private val repository: GestureSettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GestureSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GestureSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen() {
    val context = LocalContext.current
    val viewModel: GestureSettingsViewModel = viewModel(
        factory = GestureSettingsViewModelFactory(GestureSettingsRepository(context))
    )
    val appsViewModel: AppsViewModel = viewModel(
        factory = AppsViewModelFactory(
            context,
            AppDatabase.getDatabase(context).appAliasDao()
        )
    )

    val swipeUpApp by viewModel.swipeUpApp.collectAsState()
    val swipeDownApp by viewModel.swipeDownApp.collectAsState()
    val swipeLeftApp by viewModel.swipeLeftApp.collectAsState()
    val swipeRightApp by viewModel.swipeRightApp.collectAsState()

    val listItems by appsViewModel.listItems.collectAsStateWithLifecycle()
    val allApps = remember(listItems) {
        listItems.mapNotNull { (it as? ListItem.App)?.appInfo }
    }

    var showAppPickerDialog by rememberSaveable { mutableStateOf(false) }
    var gestureToSet by remember { mutableStateOf<((String) -> Unit)?>(null) }

    if (showAppPickerDialog) {
        AppPickerDialog(
            apps = allApps,
            onDismiss = { showAppPickerDialog = false },
            onAppSelected = {
                gestureToSet?.invoke(it.packageName)
                showAppPickerDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gesture Settings") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { 
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowUpward,
                    gestureName = "Swipe Up", 
                    assignedAppPackage = swipeUpApp,
                    allApps = allApps,
                    onChangeClick = { 
                        gestureToSet = viewModel::setSwipeUpApp
                        showAppPickerDialog = true 
                    },
                    onClearClick = { viewModel.clearSwipeUpApp() }
                )
            }
            item { 
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowDownward,
                    gestureName = "Swipe Down", 
                    assignedAppPackage = swipeDownApp,
                    allApps = allApps,
                    onChangeClick = { 
                        gestureToSet = viewModel::setSwipeDownApp
                        showAppPickerDialog = true 
                    },
                    onClearClick = { viewModel.clearSwipeDownApp() }
                )
            }
            item { 
                GesturePreference(
                    gestureIcon = Icons.AutoMirrored.Filled.ArrowForward,
                    gestureName = "Swipe Left", 
                    assignedAppPackage = swipeLeftApp,
                    allApps = allApps,
                    onChangeClick = { 
                        gestureToSet = viewModel::setSwipeLeftApp
                        showAppPickerDialog = true 
                    },
                    onClearClick = { viewModel.clearSwipeLeftApp() }
                )
            }
            item { 
                GesturePreference(
                    gestureIcon = Icons.AutoMirrored.Filled.ArrowForward,
                    gestureName = "Swipe Right", 
                    assignedAppPackage = swipeRightApp,
                    allApps = allApps,
                    onChangeClick = { 
                        gestureToSet = viewModel::setSwipeRightApp
                        showAppPickerDialog = true 
                    },
                    onClearClick = { viewModel.clearSwipeRightApp() }
                )
            }
        }
    }
}

@Composable
fun GesturePreference(
    gestureIcon: ImageVector,
    gestureName: String,
    assignedAppPackage: String?,
    allApps: List<AppInfo>,
    onChangeClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val context = LocalContext.current
    val assignedAppInfo = allApps.find { it.packageName == assignedAppPackage }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(gestureIcon, contentDescription = gestureName, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(gestureName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (assignedAppInfo != null) {
                    val appIcon = remember(assignedAppInfo.packageName) {
                        try {
                            context.packageManager.getApplicationIcon(assignedAppInfo.packageName)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    appIcon?.let {
                        Image(
                            bitmap = it.toBitmap().asImageBitmap(),
                            contentDescription = "App Icon",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(assignedAppInfo.label, modifier = Modifier.weight(1f))
                } else {
                    Text("Not set", modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (assignedAppInfo != null) {
                    IconButton(onClick = onClearClick) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(onClick = onChangeClick) {
                    Text("Change")
                }
            }
        }
    }
}

@Composable
fun AppPickerDialog(apps: List<AppInfo>, onDismiss: () -> Unit, onAppSelected: (AppInfo) -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select an App") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppSelected(app) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val appIcon = remember(app.packageName) {
                            try {
                                context.packageManager.getApplicationIcon(app.packageName)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        appIcon?.let {
                            Image(
                                bitmap = it.toBitmap().asImageBitmap(),
                                contentDescription = "App Icon",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(app.label)
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
