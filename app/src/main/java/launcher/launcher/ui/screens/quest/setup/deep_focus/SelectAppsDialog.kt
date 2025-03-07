package launcher.launcher.ui.screens.quest.setup.deep_focus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppSelectionDialog(
    apps: List<Pair<String, String>>, // (appName, packageName) pairs
    selectedApps: MutableList<String>, // State for selected package names
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Unrestricted Apps") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp) // Limit height to avoid overflow
            ) {
                items(apps) { (appName, packageName) ->
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