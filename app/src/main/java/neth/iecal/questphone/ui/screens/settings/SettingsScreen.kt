package neth.iecal.questphone.ui.screens.settings

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import neth.iecal.questphone.data.quest.CalendarInfo
import neth.iecal.questphone.services.CalendarSyncService
import neth.iecal.questphone.utils.CalendarSyncScheduler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.navigation.NavController
import neth.iecal.questphone.ui.screens.settings.CheckpointDialog
import java.util.Calendar
import java.util.Date
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.screens.onboard.SelectAppsModes
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import neth.iecal.questphone.receivers.AdminReceiver
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )

    @Composable
    fun UninstallProtectionSwitch() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, AdminReceiver::class.java)
        var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(componentName)) }

        // This will refresh the status when the user returns to the screen
        LaunchedEffect(Unit) {
            isAdminActive = dpm.isAdminActive(componentName)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Uninstall Protection")
            Switch(
                checked = isAdminActive,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable this to prevent accidental uninstallation of the app.")
                        }
                        context.startActivity(intent)
                    } else {
                        dpm.removeActiveAdmin(componentName)
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val settings by viewModel.settings.collectAsState()

            SettingSwitch(
                title = "Edit permission",
                isChecked = settings.isEditingEnabled,
                enabled = !settings.isSettingsLocked
            ) {
                viewModel.onEditingPermissionChanged(it)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Checkpoint Section
            Text(
                "Checkpoints",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            var showCheckpointDialog by remember { mutableStateOf(false) }

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
                        viewModel.createCheckpoint(checkpointName, comments)
                        showCheckpointDialog = false
                        Toast.makeText(context, "Checkpoint created: $checkpointName", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Unplanned Break Reasons Section
            var showAddReasonDialog by remember { mutableStateOf(false) }
            var newReason by remember { mutableStateOf("") }

            Column {
                Text(
                    "Unplanned Break Reasons",
                    style = MaterialTheme.typography.titleMedium,
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
                                    viewModel.updateUnplannedBreakReasons(updatedReasons)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete reason")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showAddReasonDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Reason", modifier = Modifier.padding(end = 8.dp))
                    Text("Add Reason")
                }

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
                                    viewModel.updateUnplannedBreakReasons(updatedReasons)
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

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "Gemini AI Assistant",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val settingsState by viewModel.settings.collectAsState()
            var apiKeyInput by rememberSaveable(settingsState.geminiApiKey) { mutableStateOf(settingsState.geminiApiKey ?: "") }

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
                    viewModel.saveGeminiApiKey(apiKeyInput)
                    Toast.makeText(context, "API Key Saved", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(top = 8.dp),
                enabled = apiKeyInput.isNotBlank() && apiKeyInput != settingsState.geminiApiKey
            ) {
                Text("Save Key")
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            val geminiResponse by viewModel.geminiResponse.collectAsState()
            var prompt by rememberSaveable { mutableStateOf("") }
            val isApiKeySet = settingsState.geminiApiKey?.isNotBlank() == true

            // Clear prompt and response if key changes
            LaunchedEffect(isApiKeySet) {
                if (!isApiKeySet) {
                    prompt = ""
                    viewModel.clearGeminiResponse()
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    prompt = it
                    // Clear previous response when user starts typing new prompt
                    if (geminiResponse != null) {
                        viewModel.clearGeminiResponse()
                    }
                },
                label = { if (isApiKeySet) "Ask Gemini..." else "Set API Key to enable" },
                modifier = Modifier.fillMaxWidth(),
                enabled = isApiKeySet
            )

            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        viewModel.generateTextWithGemini(prompt)
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
                enabled = prompt.isNotBlank() && isApiKeySet
            ) {
                Text("Generate")
            }

            geminiResponse?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { navController.navigate(Screen.GestureSettings.route) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !settings.isSettingsLocked
            ) {
                Text("Configure Gestures")
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Quest Filter Settings for HomeScreen + Dialog
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
                onCheckedChange = { viewModel.updateShowRepeatingQuestsInDialog(it) }
            )

            SettingSwitch(
                title = "Show Cloned Quests",
                isChecked = settings.showClonedQuestsInDialog,
                enabled = !settings.isSettingsLocked,
                onCheckedChange = { viewModel.updateShowClonedQuestsInDialog(it) }
            )

            SettingSwitch(
                title = "Show One-Time Quests",
                isChecked = settings.showOneTimeQuestsInDialog,
                enabled = !settings.isSettingsLocked,
                onCheckedChange = { viewModel.updateShowOneTimeQuestsInDialog(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(context, AdminReceiver::class.java)
            val isAdminActive by remember { mutableStateOf(dpm.isAdminActive(componentName)) }

            SettingSwitch(
                title = "Uninstall Protection",
                isChecked = isAdminActive,
                enabled = !settings.isSettingsLocked,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable this to prevent accidental uninstallation of the app.")
                        }
                        context.startActivity(intent)
                    } else {
                        dpm.removeActiveAdmin(componentName)
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            AccessibilityProtectionSwitch(enabled = !settings.isSettingsLocked)

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            var showLockDialog by rememberSaveable { mutableStateOf(false) }
            var showUnlockDialog by rememberSaveable { mutableStateOf(false) }

            if (showLockDialog) {
                LockSettingsDialog(
                    onDismiss = { showLockDialog = false },
                    onConfirm = { password, days ->
                        val calendar = Calendar.getInstance()
                        val lockoutEndDate = if (days > 0) {
                            calendar.add(Calendar.DAY_OF_YEAR, days)
                            calendar.timeInMillis
                        } else {
                            null
                        }
                        viewModel.onSettingsLockChanged(true, password.ifEmpty { null }, lockoutEndDate)
                        showLockDialog = false
                    }
                )
            }

            if (showUnlockDialog) {
                UnlockSettingsDialog(
                    onDismiss = { showUnlockDialog = false },
                    onConfirm = { password ->
                        if (password == settings.settingsLockPassword) {
                            viewModel.onSettingsLockChanged(false, null, null)
                            showUnlockDialog = false
                        } else {
                            Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            SettingSwitch(
                title = "Lock Settings",
                isChecked = settings.isSettingsLocked,
                onCheckedChange = {
                    if (settings.isSettingsLocked) {
                        val lockoutEndDate = settings.settingsLockoutEndDate
                        if (lockoutEndDate != null && System.currentTimeMillis() < lockoutEndDate) {
                            Toast.makeText(context, "Settings are locked until ${Date(lockoutEndDate)}", Toast.LENGTH_LONG).show()
                        } else if (settings.settingsLockPassword != null) {
                            showUnlockDialog = true
                        } else {
                            viewModel.onSettingsLockChanged(false, null, null)
                        }
                    } else {
                        showLockDialog = true
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Calendar Sync Settings
            Text(
                "Calendar Sync Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            var showTimePicker by remember { mutableStateOf(false) }
            val timePickerState = rememberTimePickerState(
                initialHour = settings.autoSyncTimeMinutes?.div(60) ?: 0,
                initialMinute = settings.autoSyncTimeMinutes?.rem(60) ?: 0,
                is24Hour = true
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
                                viewModel.updateAutoSyncTime(totalMinutes)
                                showTimePicker = false
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                    }
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
                onClick = { viewModel.updateAutoSyncTime(null) },
                enabled = settings.autoSyncTimeMinutes != null
            ) {
                Text("Disable Auto-sync")
            }

            // Test sync button
            Button(
                onClick = {
                    CalendarSyncScheduler.triggerImmediateSync(context)
                    Toast.makeText(context, "Calendar sync started", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Test Sync Now")
            }

            // Calendar selection
            var showCalendarDialog by remember { mutableStateOf(false) }
            var availableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }

            LaunchedEffect(Unit) {
                val calendarSyncService = CalendarSyncService(context, viewModel.getSettingsRepository())
                availableCalendars = calendarSyncService.getAvailableCalendars()
            }

            if (showCalendarDialog) {
                CalendarSelectionDialog(
                    availableCalendars = availableCalendars,
                    selectedCalendars = settings.selectedCalendars,
                    onDismiss = { showCalendarDialog = false },
                    onConfirm = { selectedIds ->
                        viewModel.updateSelectedCalendars(selectedIds)
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
                    if (settings.selectedCalendars.isEmpty()) "All"
                    else "${settings.selectedCalendars.size} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

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

@Composable
fun UninstallProtectionSwitch() {
    val context = LocalContext.current
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val componentName = ComponentName(context, AdminReceiver::class.java)
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(componentName)) }

    // This will refresh the status when the user returns to the screen
    LaunchedEffect(Unit) {
        isAdminActive = dpm.isAdminActive(componentName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Uninstall Protection")
        Switch(
            checked = isAdminActive,
            onCheckedChange = { isChecked ->
                if (isChecked) {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable this to prevent accidental uninstallation of the app.")
                    }
                    context.startActivity(intent)
                } else {
                    dpm.removeActiveAdmin(componentName)
                }
                // The state will be updated by LaunchedEffect when returning to the screen
            }
        )
    }
}

@Composable
fun AccessibilityProtectionSwitch(enabled: Boolean) {
    val context = LocalContext.current
    var isServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    LaunchedEffect(Unit) {
        isServiceEnabled = isAccessibilityServiceEnabled(context)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Accessibility Protection")
        Switch(
            checked = isServiceEnabled,
            enabled = enabled,
            onCheckedChange = { isChecked ->
                if (isChecked) {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } else {
                    // Cannot be disabled programmatically, user must do it from settings
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    Toast.makeText(context, "Please disable the service manually", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(context, neth.iecal.questphone.services.AccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    if (enabledServices == null || enabledServices.isEmpty()) {
        return false
    }
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
        val componentName = ComponentName.unflattenFromString(splitter.next())
        if (componentName != null && componentName == expectedComponentName) {
            return true
        }
    }
    return false
}

@Composable
fun SettingSwitch(title: String, isChecked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title)
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun LockSettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String, days: Int) -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf("") }
    val isError = remember(password, confirmPassword) { password != confirmPassword }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lock Settings") },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isError
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isError,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (isError) {
                    Text(
                        text = "Passwords do not match",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it.filter { char -> char.isDigit() } },
                    label = { Text("Lockout duration (days, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(password, days.toIntOrNull() ?: 0)
                },
                enabled = !isError && (password.isNotEmpty() || days.isNotEmpty())
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CalendarSelectionDialog(
    availableCalendars: List<CalendarInfo>,
    selectedCalendars: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var tempSelectedCalendars by remember { mutableStateOf(selectedCalendars) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Calendars") },
        text = {
            LazyColumn {
                items(availableCalendars) { calendar ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                val newSelection = if (calendar.id in tempSelectedCalendars) {
                                    tempSelectedCalendars - calendar.id
                                } else {
                                    tempSelectedCalendars + calendar.id
                                }
                                tempSelectedCalendars = newSelection
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = calendar.id in tempSelectedCalendars,
                            onCheckedChange = { isChecked ->
                                val newSelection = if (isChecked) {
                                    tempSelectedCalendars + calendar.id
                                } else {
                                    tempSelectedCalendars - calendar.id
                                }
                                tempSelectedCalendars = newSelection
                            }
                        )
                        Text(
                            text = "${calendar.name} (${calendar.accountName})",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tempSelectedCalendars) }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UnlockSettingsDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock Settings") },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) }
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
