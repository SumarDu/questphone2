package neth.iecal.questphone.widget

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.data.timer.TimerService

/**
 * Dialog activity for submitting a deferred unplanned break reason from the widget
 */
class WidgetDeferredReasonDialogActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make this activity appear as a dialog over other apps
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        setContent {
            MaterialTheme {
                DeferredReasonDialog(
                    onSubmit = { reason ->
                        submitReason(reason)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
    
    private fun submitReason(reason: String) {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_SUBMIT_UNPLANNED_BREAK_REASON
            putExtra(TimerService.EXTRA_UNPLANNED_BREAK_REASON, reason)
        }
        startService(intent)
    }
}

@Composable
fun DeferredReasonDialog(
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What was the reason?") },
        text = {
            Column {
                Text("Please enter a reason for your unplanned break:")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (reason.isNotBlank()) {
                    onSubmit(reason)
                }
            }) {
                Text("Submit")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
