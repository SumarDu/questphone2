package neth.iecal.questphone.widget

import android.app.Activity
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
import java.util.concurrent.TimeUnit

/**
 * Dialog activity for adding time to the current quest from the widget
 */
class WidgetAddTimeDialogActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make this activity appear as a dialog over other apps
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        setContent {
            MaterialTheme {
                AddTimeDialog(
                    onAddTime = { minutes ->
                        addTime(minutes)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
    
    private fun addTime(minutes: Int) {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_ADD_TIME
            putExtra(TimerService.EXTRA_TIME_TO_ADD, TimeUnit.MINUTES.toMillis(minutes.toLong()))
        }
        startService(intent)
    }
}

@Composable
fun AddTimeDialog(
    onAddTime: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Time") },
        text = { Text("Select how much time to add to the current quest.") },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onAddTime(5) }) {
                    Text("5m")
                }
                Button(onClick = { onAddTime(10) }) {
                    Text("10m")
                }
                Button(onClick = { onAddTime(15) }) {
                    Text("15m")
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
