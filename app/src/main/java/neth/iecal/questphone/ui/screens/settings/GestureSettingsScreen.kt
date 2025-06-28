package neth.iecal.questphone.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.data.preferences.GestureSettingsRepository
import neth.iecal.questphone.ui.screens.launcher.AppInfo
import neth.iecal.questphone.ui.screens.launcher.AppsViewModel
import neth.iecal.questphone.ui.screens.launcher.AppsViewModelFactory

class GestureSettingsViewModel(private val repository: GestureSettingsRepository) : ViewModel() {
    val swipeUpApp = repository.swipeUpApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val swipeDownApp = repository.swipeDownApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val swipeLeftApp = repository.swipeLeftApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val swipeRightApp = repository.swipeRightApp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSwipeUpApp(packageName: String) = viewModelScope.launch { repository.saveSwipeUpApp(packageName) }
    fun setSwipeDownApp(packageName: String) = viewModelScope.launch { repository.saveSwipeDownApp(packageName) }
    fun setSwipeLeftApp(packageName: String) = viewModelScope.launch { repository.saveSwipeLeftApp(packageName) }
    fun setSwipeRightApp(packageName: String) = viewModelScope.launch { repository.saveSwipeRightApp(packageName) }
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
    val allApps by appsViewModel.apps.collectAsState()

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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            GestureRow("Swipe Up", swipeUpApp ?: "Not set") { 
                gestureToSet = viewModel::setSwipeUpApp
                showAppPickerDialog = true 
            }
            GestureRow("Swipe Down", swipeDownApp ?: "Not set") { 
                gestureToSet = viewModel::setSwipeDownApp
                showAppPickerDialog = true 
            }
            GestureRow("Swipe Left", swipeLeftApp ?: "Not set") { 
                gestureToSet = viewModel::setSwipeLeftApp
                showAppPickerDialog = true 
            }
            GestureRow("Swipe Right", swipeRightApp ?: "Not set") { 
                gestureToSet = viewModel::setSwipeRightApp
                showAppPickerDialog = true 
            }
        }
    }
}

@Composable
fun GestureRow(gestureName: String, assignedApp: String, onChangeClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$gestureName: $assignedApp")
        Button(onClick = onChangeClick) {
            Text("Change")
        }
    }
}

@Composable
fun AppPickerDialog(apps: List<AppInfo>, onDismiss: () -> Unit, onAppSelected: (AppInfo) -> Unit) {
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
