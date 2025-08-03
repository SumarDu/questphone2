package neth.iecal.questphone.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheckpointDialog(
    onDismiss: () -> Unit,
    onConfirm: (checkpointName: String, comments: String?) -> Unit
) {
    var checkpointName by rememberSaveable { mutableStateOf("") }
    var comments by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Checkpoint") },
        text = {
            Column {
                OutlinedTextField(
                    value = checkpointName,
                    onValueChange = { checkpointName = it },
                    label = { Text("Checkpoint Name") },
                    placeholder = { Text("e.g., update of reward system") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Comments (optional)") },
                    placeholder = { Text("Additional details...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (checkpointName.isNotBlank()) {
                        onConfirm(checkpointName.trim(), comments.takeIf { it.isNotBlank() }?.trim())
                    }
                },
                enabled = checkpointName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
