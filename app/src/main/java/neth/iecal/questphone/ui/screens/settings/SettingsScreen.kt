package neth.iecal.questphone.ui.screens.settings

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

            val isEditingEnabled = settings.isQuestCreationEnabled &&
                settings.isQuestDeletionEnabled &&
                settings.isItemCreationEnabled &&
                settings.isItemDeletionEnabled

            SettingSwitch(
                title = "Editing permission",
                isChecked = isEditingEnabled,
                enabled = !settings.isSettingsLocked
            ) {
                viewModel.onEditingPermissionChanged(it)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))



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

            Button(onClick = { navController.navigate(Screen.GestureSettings.route) }, enabled = !settings.isSettingsLocked) {
                Text("Configure Gestures")
            }

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
