package neth.iecal.questphone.ui.screens.quest.setup.deep_focus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.data.AppInfo

@Composable
fun SelectAppsDialog(
    apps: MutableState<List<AppInfo>>, // (appName, packageName) pairs
    selectedApps: MutableList<String>, // State for selected package names
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Unrestricted Apps") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                val query = remember { mutableStateOf("") }
                OutlinedTextField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    label = { Text("Search apps") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                val filtered = remember(apps.value, query.value) {
                    val q = query.value.trim()
                    if (q.isEmpty()) apps.value
                    else apps.value.filter { (name, pkg) ->
                        name.contains(q, ignoreCase = true) || pkg.contains(q, ignoreCase = true)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(filtered) { (appName, packageName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedApps.contains(packageName)) {
                                        selectedApps.remove(packageName)
                                    } else {
                                        selectedApps.add(packageName)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedApps.contains(packageName),
                                onCheckedChange = {
                                    if (it) selectedApps.add(packageName) else selectedApps.remove(packageName)
                                }
                            )
                            Text(
                                text = appName,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}