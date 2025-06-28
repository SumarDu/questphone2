package neth.iecal.questphone.ui.screens.settings

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as Application)
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            val settings by viewModel.settings.collectAsState()

            SettingSwitch(title = "Enable Quest Creation", isChecked = settings.isQuestCreationEnabled, enabled = !settings.isSettingsLocked) { viewModel.onQuestCreationChanged(it) }
            SettingSwitch(title = "Enable Quest Deletion", isChecked = settings.isQuestDeletionEnabled, enabled = !settings.isSettingsLocked) { viewModel.onQuestDeletionChanged(it) }
            SettingSwitch(title = "Enable Item Creation", isChecked = settings.isItemCreationEnabled, enabled = !settings.isSettingsLocked) { viewModel.onItemCreationChanged(it) }
            SettingSwitch(title = "Enable Item Deletion", isChecked = settings.isItemDeletionEnabled, enabled = !settings.isSettingsLocked) { viewModel.onItemDeletionChanged(it) }

            Button(onClick = { navController.navigate(Screen.GestureSettings.route) }) {
                Text("Configure Gestures")
            }

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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Distraction Apps")
            }
        }
    }
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
