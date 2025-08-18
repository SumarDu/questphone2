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
    // New gestures
    val twoFingerSwipeUpApp = repository.twoFingerSwipeUpApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val twoFingerSwipeDownApp = repository.twoFingerSwipeDownApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val doubleTapBottomLeftApp = repository.doubleTapBottomLeftApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val doubleTapBottomRightApp = repository.doubleTapBottomRightApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val longPressApp = repository.longPressApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val edgeLeftSwipeUpApp = repository.edgeLeftSwipeUpApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val edgeLeftSwipeDownApp = repository.edgeLeftSwipeDownApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val edgeRightSwipeUpApp = repository.edgeRightSwipeUpApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val edgeRightSwipeDownApp = repository.edgeRightSwipeDownApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    // Bottom applet
    val bottomRightMode = repository.doubleTapBottomRightMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "single")
    val bottomAppletApps = repository.bottomAppletApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSwipeUpApp(packageName: String) = viewModelScope.launch { repository.saveSwipeUpApp(packageName) }
    fun setSwipeDownApp(packageName: String) = viewModelScope.launch { repository.saveSwipeDownApp(packageName) }
    fun setSwipeLeftApp(packageName: String) = viewModelScope.launch { repository.saveSwipeLeftApp(packageName) }
    fun setSwipeRightApp(packageName: String) = viewModelScope.launch { repository.saveSwipeRightApp(packageName) }
    // New setters
    fun setTwoFingerSwipeUpApp(packageName: String) = viewModelScope.launch { repository.saveTwoFingerSwipeUpApp(packageName) }
    fun setTwoFingerSwipeDownApp(packageName: String) = viewModelScope.launch { repository.saveTwoFingerSwipeDownApp(packageName) }
    fun setDoubleTapBottomLeftApp(packageName: String) = viewModelScope.launch { repository.saveDoubleTapBottomLeftApp(packageName) }
    fun setDoubleTapBottomRightApp(packageName: String) = viewModelScope.launch { repository.saveDoubleTapBottomRightApp(packageName) }
    fun setLongPressApp(packageName: String) = viewModelScope.launch { repository.saveLongPressApp(packageName) }
    fun setEdgeLeftSwipeUpApp(packageName: String) = viewModelScope.launch { repository.saveEdgeLeftSwipeUpApp(packageName) }
    fun setEdgeLeftSwipeDownApp(packageName: String) = viewModelScope.launch { repository.saveEdgeLeftSwipeDownApp(packageName) }
    fun setEdgeRightSwipeUpApp(packageName: String) = viewModelScope.launch { repository.saveEdgeRightSwipeUpApp(packageName) }
    fun setEdgeRightSwipeDownApp(packageName: String) = viewModelScope.launch { repository.saveEdgeRightSwipeDownApp(packageName) }
    // Bottom applet setters
    fun setBottomRightMode(mode: String) = viewModelScope.launch { repository.saveDoubleTapBottomRightMode(mode) }
    fun setBottomAppletApps(packages: List<String>) = viewModelScope.launch { repository.saveBottomAppletApps(packages) }

    fun clearSwipeUpApp() = viewModelScope.launch { repository.saveSwipeUpApp("") }
    fun clearSwipeDownApp() = viewModelScope.launch { repository.saveSwipeDownApp("") }
    fun clearSwipeLeftApp() = viewModelScope.launch { repository.saveSwipeLeftApp("") }
    fun clearSwipeRightApp() = viewModelScope.launch { repository.saveSwipeRightApp("") }
    // New clearers
    fun clearTwoFingerSwipeUpApp() = viewModelScope.launch { repository.saveTwoFingerSwipeUpApp("") }
    fun clearTwoFingerSwipeDownApp() = viewModelScope.launch { repository.saveTwoFingerSwipeDownApp("") }
    fun clearDoubleTapBottomLeftApp() = viewModelScope.launch { repository.saveDoubleTapBottomLeftApp("") }
    fun clearDoubleTapBottomRightApp() = viewModelScope.launch { repository.saveDoubleTapBottomRightApp("") }
    fun clearLongPressApp() = viewModelScope.launch { repository.saveLongPressApp("") }
    fun clearEdgeLeftSwipeUpApp() = viewModelScope.launch { repository.saveEdgeLeftSwipeUpApp("") }
    fun clearEdgeLeftSwipeDownApp() = viewModelScope.launch { repository.saveEdgeLeftSwipeDownApp("") }
    fun clearEdgeRightSwipeUpApp() = viewModelScope.launch { repository.saveEdgeRightSwipeUpApp("") }
    fun clearEdgeRightSwipeDownApp() = viewModelScope.launch { repository.saveEdgeRightSwipeDownApp("") }
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
    // New gestures
    val twoFingerSwipeUpApp by viewModel.twoFingerSwipeUpApp.collectAsState()
    val twoFingerSwipeDownApp by viewModel.twoFingerSwipeDownApp.collectAsState()
    val doubleTapBottomLeftApp by viewModel.doubleTapBottomLeftApp.collectAsState()
    val doubleTapBottomRightApp by viewModel.doubleTapBottomRightApp.collectAsState()
    val longPressApp by viewModel.longPressApp.collectAsState()
    val edgeLeftSwipeUpApp by viewModel.edgeLeftSwipeUpApp.collectAsState()
    val edgeLeftSwipeDownApp by viewModel.edgeLeftSwipeDownApp.collectAsState()
    val edgeRightSwipeUpApp by viewModel.edgeRightSwipeUpApp.collectAsState()
    val edgeRightSwipeDownApp by viewModel.edgeRightSwipeDownApp.collectAsState()
    val bottomRightMode by viewModel.bottomRightMode.collectAsState()
    val configuredAppletApps by viewModel.bottomAppletApps.collectAsState()

    val listItems by appsViewModel.listItems.collectAsStateWithLifecycle()
    val allApps = remember(listItems) {
        listItems.mapNotNull { (it as? ListItem.App)?.appInfo }
    }

    var showAppPickerDialog by rememberSaveable { mutableStateOf(false) }
    var gestureToSet by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var showAppletConfigDialog by rememberSaveable { mutableStateOf(false) }

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
    if (showAppletConfigDialog) {
        BottomAppletConfigDialog(
            apps = allApps,
            initiallySelected = configuredAppletApps,
            onDismiss = { showAppletConfigDialog = false },
            onSave = { selected ->
                viewModel.setBottomAppletApps(selected)
                showAppletConfigDialog = false
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
            // New gesture items
            item {
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowUpward,
                    gestureName = "Two-finger Swipe Up",
                    assignedAppPackage = twoFingerSwipeUpApp,
                    allApps = allApps,
                    onChangeClick = {
                        gestureToSet = viewModel::setTwoFingerSwipeUpApp
                        showAppPickerDialog = true
                    },
                    onClearClick = { viewModel.clearTwoFingerSwipeUpApp() }
                )
            }
            item {
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowDownward,
                    gestureName = "Two-finger Swipe Down",
                    assignedAppPackage = twoFingerSwipeDownApp,
                    allApps = allApps,
                    onChangeClick = {
                        gestureToSet = viewModel::setTwoFingerSwipeDownApp
                        showAppPickerDialog = true
                    },
                    onClearClick = { viewModel.clearTwoFingerSwipeDownApp() }
                )
            }
            item {
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowDownward,
                    gestureName = "Double Tap Bottom-Left",
                    assignedAppPackage = doubleTapBottomLeftApp,
                    allApps = allApps,
                    onChangeClick = {
                        gestureToSet = viewModel::setDoubleTapBottomLeftApp
                        showAppPickerDialog = true
                    },
                    onClearClick = { viewModel.clearDoubleTapBottomLeftApp() }
                )
            }
            item {
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowDownward,
                    gestureName = "Double Tap Bottom-Right",
                    assignedAppPackage = doubleTapBottomRightApp,
                    allApps = allApps,
                    onChangeClick = {
                        gestureToSet = viewModel::setDoubleTapBottomRightApp
                        showAppPickerDialog = true
                    },
                    onClearClick = { viewModel.clearDoubleTapBottomRightApp() }
                )
            }
            // Bottom applet configuration block
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bottom-Right Double Tap Action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = bottomRightMode != "applet",
                                onClick = { viewModel.setBottomRightMode("single") }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Launch assigned app")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = bottomRightMode == "applet",
                                onClick = { viewModel.setBottomRightMode("applet") }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Open quick applet")
                        }
                        if (bottomRightMode == "applet") {
                            Spacer(Modifier.height(12.dp))
                            Text("Selected apps: ${configuredAppletApps.size}/6")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { showAppletConfigDialog = true }) {
                                Text("Configure Quick Applet")
                            }
                        } else {
                            Spacer(Modifier.height(12.dp))
                            Text("Assigned app")
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                val assigned = doubleTapBottomRightApp
                                val appInfo = allApps.find { it.packageName == assigned }
                                val appIcon = appInfo?.let {
                                    remember(it.packageName) {
                                        try {
                                            context.packageManager.getApplicationIcon(it.packageName)
                                        } catch (e: Exception) { null }
                                    }
                                }
                                if (appInfo != null) {
                                    appIcon?.let {
                                        Image(
                                            bitmap = it.toBitmap().asImageBitmap(),
                                            contentDescription = "App Icon",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(appInfo.label, modifier = Modifier.weight(1f))
                                } else {
                                    Text("Not set", modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                if (!doubleTapBottomRightApp.isNullOrEmpty()) {
                                    IconButton(onClick = { viewModel.clearDoubleTapBottomRightApp() }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                                Button(onClick = {
                                    gestureToSet = viewModel::setDoubleTapBottomRightApp
                                    showAppPickerDialog = true
                                }) {
                                    Text("Change")
                                }
                            }
                        }
                    }
                }
            }
            item {
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowDownward,
                    gestureName = "Long Press",
                    assignedAppPackage = longPressApp,
                    allApps = allApps,
                    onChangeClick = {
                        gestureToSet = viewModel::setLongPressApp
                        showAppPickerDialog = true
                    },
                    onClearClick = { viewModel.clearLongPressApp() }
                )
            }
            item {
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowUpward,
                    gestureName = "Left Edge Swipe Up",
                    assignedAppPackage = edgeLeftSwipeUpApp,
                    allApps = allApps,
                    onChangeClick = {
                        gestureToSet = viewModel::setEdgeLeftSwipeUpApp
                        showAppPickerDialog = true
                    },
                    onClearClick = { viewModel.clearEdgeLeftSwipeUpApp() }
                )
            }
            item {
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowDownward,
                    gestureName = "Left Edge Swipe Down",
                    assignedAppPackage = edgeLeftSwipeDownApp,
                    allApps = allApps,
                    onChangeClick = {
                        gestureToSet = viewModel::setEdgeLeftSwipeDownApp
                        showAppPickerDialog = true
                    },
                    onClearClick = { viewModel.clearEdgeLeftSwipeDownApp() }
                )
            }
            item {
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowUpward,
                    gestureName = "Right Edge Swipe Up",
                    assignedAppPackage = edgeRightSwipeUpApp,
                    allApps = allApps,
                    onChangeClick = {
                        gestureToSet = viewModel::setEdgeRightSwipeUpApp
                        showAppPickerDialog = true
                    },
                    onClearClick = { viewModel.clearEdgeRightSwipeUpApp() }
                )
            }
            item {
                GesturePreference(
                    gestureIcon = Icons.Filled.ArrowDownward,
                    gestureName = "Right Edge Swipe Down",
                    assignedAppPackage = edgeRightSwipeDownApp,
                    allApps = allApps,
                    onChangeClick = {
                        gestureToSet = viewModel::setEdgeRightSwipeDownApp
                        showAppPickerDialog = true
                    },
                    onClearClick = { viewModel.clearEdgeRightSwipeDownApp() }
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
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select an App") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filtered, key = { "${it.packageName}:${it.user.hashCode()}" }) { app ->
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

@Composable
fun BottomAppletConfigDialog(
    apps: List<AppInfo>,
    initiallySelected: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val maxSelection = 6
    val selected = remember { mutableStateListOf<String>().apply { addAll(initiallySelected.distinct()) } }

    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Quick Applet") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Selected: ${selected.size}/$maxSelection", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filtered) { app ->
                        val isChecked = selected.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        selected.remove(app.packageName)
                                    } else if (selected.size < maxSelection) {
                                        selected.add(app.packageName)
                                    }
                                }
                                .padding(vertical = 10.dp),
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
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(app.label, modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (selected.size < maxSelection && !selected.contains(app.packageName)) {
                                            selected.add(app.packageName)
                                        }
                                    } else {
                                        selected.remove(app.packageName)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selected.toList()) }, enabled = selected.isNotEmpty()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
