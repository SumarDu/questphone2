package neth.iecal.questphone.widget

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.data.timer.TimerService

/**
 * Dialog activity for starting an unplanned break from the widget
 */
class WidgetUnplannedBreakDialogActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make this activity appear as a dialog over other apps
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        setContent {
            MaterialTheme {
                UnplannedBreakDialog(
                    onStart = { reason ->
                        startUnplannedBreak(reason)
                        finish()
                    },
                    onLater = {
                        startUnplannedBreakLater()
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
    
    private fun startUnplannedBreak(reason: String) {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START_UNPLANNED_BREAK
            putExtra("UNPLANNED_BREAK_REASON", reason)
        }
        startService(intent)
    }
    
    private fun startUnplannedBreakLater() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START_UNPLANNED_BREAK
            putExtra("UNPLANNED_BREAK_REASON", TimerService.LATER_MARKER)
        }
        startService(intent)
    }
}

@Composable
fun UnplannedBreakDialog(
    onStart: (String) -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState()
    
    var reason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unplanned Break") },
        text = {
            Column {
                Text("Select or enter a reason for the unplanned break:")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (settings.unplannedBreakReasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Or choose from your list:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        items(settings.unplannedBreakReasons) { savedReason ->
                            Text(
                                text = savedReason,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reason = savedReason }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onStart(reason) }) {
                    Text("Start")
                }
                Button(onClick = onLater) {
                    Text("Later")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
