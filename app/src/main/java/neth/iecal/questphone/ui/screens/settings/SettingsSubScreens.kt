package neth.iecal.questphone.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.app.Application
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.screens.onboard.SelectAppsModes
import neth.iecal.questphone.data.quest.CalendarInfo
import neth.iecal.questphone.services.CalendarSyncService
import neth.iecal.questphone.utils.CalendarSyncScheduler
import neth.iecal.questphone.data.timer.TimerService
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import java.util.Date
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCheckpointsScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )

    var showCheckpointDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Checkpoints") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Create snapshots of your app state to restore or analyze later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = { showCheckpointDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Checkpoint")
            }

            if (showCheckpointDialog) {
                CheckpointDialog(
                    onDismiss = { showCheckpointDialog = false },
                    onConfirm = { checkpointName, comments ->
                        settingsViewModel.createCheckpoint(checkpointName, comments)
                        showCheckpointDialog = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProtectionScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )

    val settings by settingsViewModel.settings.collectAsState()

    var showLockDialog = remember { mutableStateOf(false) }
    var showUnlockDialog = remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Protection") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Secure the app and control access to critical features.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Edit permission
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingSwitch(
                title = "Edit permission",
                isChecked = settings.isEditingEnabled,
                enabled = !settings.isSettingsLocked
            ) {
                settingsViewModel.onEditingPermissionChanged(it)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Protection options
            Text(
                text = "Protection Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Uninstall protection (Device Admin)
            UninstallProtectionSwitch()

            // Accessibility Protection
            AccessibilityProtectionSwitch(enabled = !settings.isSettingsLocked)

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Lock / Unlock settings
            Text(
                text = "Lock Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (!settings.isSettingsLocked) {
                Button(onClick = { showLockDialog.value = true }) {
                    Text("Lock Settings")
                }
            } else {
                Button(onClick = { showUnlockDialog.value = true }) {
                    Text("Unlock Settings")
                }
            }

            if (showLockDialog.value) {
                LockSettingsDialog(
                    onDismiss = { showLockDialog.value = false },
                    onConfirm = { password, days ->
                        // Calculate lockout end date if days provided
                        val lockoutEnd = if (days > 0) {
                            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }.timeInMillis
                        } else null
                        settingsViewModel.onSettingsLockChanged(true, password.ifBlank { null }, lockoutEnd)
                        showLockDialog.value = false
                    }
                )
            }

            if (showUnlockDialog.value) {
                UnlockSettingsDialog(
                    onDismiss = { showUnlockDialog.value = false },
                    onConfirm = { _password ->
                        // For now simply unlock; repository may validate internally if needed
                        settingsViewModel.onSettingsLockChanged(false, null, null)
                        showUnlockDialog.value = false
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Manage Distraction Apps
            Button(
                onClick = {
                    navController.navigate(Screen.SelectApps.route + SelectAppsModes.ALLOW_ADD_AND_REMOVE.ordinal)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !settings.isSettingsLocked
            ) {
                Text("Manage Distraction Apps")
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOverduePenaltiesScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )
    val settings by settingsViewModel.settings.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Overdue Penalties") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Apply consequences for tasks finished after their due time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingSwitch(
                title = "Enable overdue penalties",
                isChecked = settings.overduePenaltyEnabled,
                enabled = !settings.isSettingsLocked,
                onCheckedChange = { settingsViewModel.setOverduePenaltyEnabled(it) }
            )

            val enabledPenaltyInputs = !settings.isSettingsLocked && settings.overduePenaltyEnabled
            var overdueWindowInput by rememberSaveable(settings.overduePenaltyWindowMinutes) { mutableStateOf(settings.overduePenaltyWindowMinutes.toString()) }
            var overdueCoinsInput by rememberSaveable(settings.overduePenaltyCoins) { mutableStateOf(settings.overduePenaltyCoins.toString()) }

            OutlinedTextField(
                value = overdueWindowInput,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }
                    overdueWindowInput = digits
                    val minutes = digits.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    if (minutes != settings.overduePenaltyWindowMinutes) {
                        settingsViewModel.setOverduePenaltyWindow(minutes)
                    }
                },
                label = { Text("Penalty window (minutes)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = enabledPenaltyInputs,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = overdueCoinsInput,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }
                    overdueCoinsInput = digits
                    val coins = digits.toIntOrNull() ?: 0
                    if (coins != settings.overduePenaltyCoins) {
                        settingsViewModel.setOverduePenaltyCoins(coins)
                    }
                },
                label = { Text("Coins per window") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = enabledPenaltyInputs,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUnplannedBreakReasonsScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )
    val settings by settingsViewModel.settings.collectAsState()

    var showAddReasonDialog by remember { mutableStateOf(false) }
    var newReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unplanned Break Reasons") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddReasonDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add reason")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Customize the reasons you can select when taking an unplanned break.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(modifier = Modifier.height(150.dp)) {
                LazyColumn {
                    items(settings.unplannedBreakReasons) { reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(reason, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val updatedReasons = settings.unplannedBreakReasons.toMutableList().apply {
                                    remove(reason)
                                }
                                settingsViewModel.updateUnplannedBreakReasons(updatedReasons)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete reason")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (showAddReasonDialog) {
                AlertDialog(
                    onDismissRequest = { showAddReasonDialog = false },
                    title = { Text("Add a new reason") },
                    text = {
                        OutlinedTextField(
                            value = newReason,
                            onValueChange = { newReason = it },
                            label = { Text("Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newReason.isNotBlank()) {
                                val updatedReasons = settings.unplannedBreakReasons.toMutableList().apply {
                                    add(newReason)
                                }
                                settingsViewModel.updateUnplannedBreakReasons(updatedReasons)
                                newReason = ""
                                showAddReasonDialog = false
                            }
                        }) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showAddReasonDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUnplannedQuestFilterScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )
    val settings by settingsViewModel.settings.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Unplanned Quest Filter") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Quest Selection Dialog Filters",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "Choose which quest types appear in the \"Select a Quest\" dialog (+ button on timer)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingSwitch(
                title = "Show Repeating Quests",
                isChecked = settings.showRepeatingQuestsInDialog,
                enabled = !settings.isSettingsLocked,
                onCheckedChange = { settingsViewModel.updateShowRepeatingQuestsInDialog(it) }
            )
            SettingSwitch(
                title = "Show Cloned Quests",
                isChecked = settings.showClonedQuestsInDialog,
                enabled = !settings.isSettingsLocked,
                onCheckedChange = { settingsViewModel.updateShowClonedQuestsInDialog(it) }
            )
            SettingSwitch(
                title = "Show One-Time Quests",
                isChecked = settings.showOneTimeQuestsInDialog,
                enabled = !settings.isSettingsLocked,
                onCheckedChange = { settingsViewModel.updateShowOneTimeQuestsInDialog(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCalendarSyncScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )
    val settings by settingsViewModel.settings.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = settings.autoSyncTimeMinutes?.div(60) ?: 0,
        initialMinute = settings.autoSyncTimeMinutes?.rem(60) ?: 0,
        is24Hour = true
    )

    var showCalendarDialog by remember { mutableStateOf(false) }
    var availableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        val calendarSyncService = CalendarSyncService(context, settingsViewModel.getSettingsRepository())
        availableCalendars = calendarSyncService.getAvailableCalendars()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Calendar Sync Settings") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Connect your calendar and set automatic sync time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Select Sync Time") },
                    text = { TimePicker(state = timePickerState) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val totalMinutes = timePickerState.hour * 60 + timePickerState.minute
                                settingsViewModel.updateAutoSyncTime(totalMinutes)
                                showTimePicker = false
                            }
                        ) { Text("Confirm") }
                    },
                    dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto-sync Time")
                Text(
                    settings.autoSyncTimeMinutes?.let { String.format("%02d:%02d", it / 60, it % 60) } ?: "Disabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = { settingsViewModel.updateAutoSyncTime(null) },
                enabled = settings.autoSyncTimeMinutes != null
            ) { Text("Disable Auto-sync") }

            Button(
                onClick = {
                    CalendarSyncScheduler.triggerImmediateSync(context)
                },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("Test Sync Now") }

            if (showCalendarDialog) {
                CalendarSelectionDialog(
                    availableCalendars = availableCalendars,
                    selectedCalendars = settings.selectedCalendars,
                    onDismiss = { showCalendarDialog = false },
                    onConfirm = { selectedIds ->
                        settingsViewModel.updateSelectedCalendars(selectedIds)
                        showCalendarDialog = false
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCalendarDialog = true }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Select Calendars to Sync")
                Text(
                    if (settings.selectedCalendars.isEmpty()) "All" else "${settings.selectedCalendars.size} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAiToolsScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )
    val settingsState by settingsViewModel.settings.collectAsState()
    var apiKeyInput by rememberSaveable(settingsState.geminiApiKey) { mutableStateOf(settingsState.geminiApiKey ?: "") }
    val geminiResponse by settingsViewModel.geminiResponse.collectAsState()
    var prompt by rememberSaveable { mutableStateOf("") }
    val isApiKeySet = settingsState.geminiApiKey?.isNotBlank() == true

    LaunchedEffect(isApiKeySet) {
        if (!isApiKeySet) {
            prompt = ""
            settingsViewModel.clearGeminiResponse()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("AI Tools Settings") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Configure AI assistants and test prompts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text("Gemini AI Assistant", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Button(
                onClick = {
                    settingsViewModel.saveGeminiApiKey(apiKeyInput)
                },
                modifier = Modifier.padding(top = 8.dp),
                enabled = apiKeyInput.isNotBlank() && apiKeyInput != settingsState.geminiApiKey
            ) { Text("Save Key") }

            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    prompt = it
                    if (geminiResponse != null) settingsViewModel.clearGeminiResponse()
                },
                label = { if (isApiKeySet) Text("Ask Gemini...") else Text("Set API Key to enable") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isApiKeySet
            )

            Button(
                onClick = { if (prompt.isNotBlank()) settingsViewModel.generateTextWithGemini(prompt) },
                modifier = Modifier.padding(top = 8.dp),
                enabled = prompt.isNotBlank() && isApiKeySet
            ) { Text("Generate") }

            geminiResponse?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupsDevScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )
    val settings by settingsViewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val timerState by neth.iecal.questphone.data.timer.TimerService.timerState.collectAsState()
    var showDevWarn by remember { mutableStateOf(false) }

    // File pickers for export and import
    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = neth.iecal.questphone.data.backup.BackupManager.buildBackup(context)
                    neth.iecal.questphone.data.backup.BackupManager.writeToUri(context, uri, content)
                    Toast.makeText(context, "Backup exported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = neth.iecal.questphone.data.backup.BackupManager.readFromUri(context, uri)
                    neth.iecal.questphone.data.backup.BackupManager.restoreFromJson(context, content)
                    Toast.makeText(context, "Backup imported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Backups & Development") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Manage backups and developer utilities.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(
                onClick = { navController.navigate(Screen.GestureSettings.route) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !settings.isSettingsLocked
            ) { Text("Configure Gestures") }

            Spacer(Modifier.height(16.dp))
            Text("Backups", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val name = neth.iecal.questphone.data.backup.BackupManager.suggestedFileName()
                        exportLauncher.launch(name)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Export Backup") }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !settings.isSettingsLocked
                ) { Text("Import Backup") }
            }

            Spacer(Modifier.height(24.dp))
            Text("Developer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Button(
                onClick = {
                    scope.launch {
                        try {
                            // Gate by timer state: allow only IDLE (INACTIVE) or Break Overdue
                            val okToProceed =
                                (timerState.mode == neth.iecal.questphone.data.timer.TimerMode.INACTIVE) ||
                                (timerState.mode == neth.iecal.questphone.data.timer.TimerMode.OVERTIME)
                            if (!okToProceed) {
                                showDevWarn = true
                                return@launch
                            }
                            // 1) Create backup to app private files
                            val content = neth.iecal.questphone.data.backup.BackupManager.buildBackup(context)
                            val fileName = neth.iecal.questphone.data.backup.BackupManager.suggestedFileName()
                            val path = neth.iecal.questphone.data.backup.BackupManager.writeToAppFiles(context, fileName, content)
                            val f = java.io.File(path)
                            val ok = f.exists() && f.length() > 0L
                            // Also try to save a user-visible copy to Downloads
                            val publicUri = neth.iecal.questphone.data.backup.BackupManager.writeToDownloads(context, fileName, content)

                            // 2) Force timer to IDLE before creating checkpoint
                            val idleIntent = android.content.Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_FORCE_IDLE
                            }
                            context.startService(idleIntent)
                            // Wait until service reports IDLE (INACTIVE) to ensure strict order
                            withTimeoutOrNull(3000) {
                                neth.iecal.questphone.data.timer.TimerService.timerState
                                    .filter { it.mode == neth.iecal.questphone.data.timer.TimerMode.INACTIVE }
                                    .first()
                            }

                            // 3) Create a checkpoint to signal developer mode
                            val checkpointName = "dev_m_start"
                            val comment: String? = null
                            settingsViewModel.createCheckpoint(checkpointName, comment)

                            // 4) Set persistent flag to show blocking screen on Home
                            context.getSharedPreferences("dev_mode", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("pending_reinstall", true).apply()

                            val toastMsg = buildString {
                                append("Developer mode engaged. ")
                                if (ok) append("Saved: $path. ") else append("Internal save failed. ")
                                if (publicUri != null) append("Also in Downloads: $publicUri") else append("Downloads save failed.")
                            }
                            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()

                            // 5) Go to Home and lock UI with prompt
                            navController.navigate(neth.iecal.questphone.ui.navigation.Screen.HomeScreen.route) {
                                popUpTo(neth.iecal.questphone.ui.navigation.Screen.SettingsBackupsDev.route) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to enter developer mode: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !settings.isSettingsLocked
            ) { Text("Enter Developer Mode") }

            if (showDevWarn) {
                AlertDialog(
                    onDismissRequest = { showDevWarn = false },
                    title = { Text("Cannot Enter Developer Mode") },
                    text = { Text("You can only enter developer mode when the timer is IDLE or Break is Overdue. Please stop the current quest/break and try again.") },
                    confirmButton = {
                        TextButton(onClick = { showDevWarn = false }) { Text("OK") }
                    }
                )
            }
        }
    }
}
