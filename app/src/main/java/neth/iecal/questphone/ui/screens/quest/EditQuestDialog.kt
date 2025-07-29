package neth.iecal.questphone.ui.screens.quest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.ui.screens.quest.setup.components.SetBaseQuest

@Composable
fun EditQuestDialog(
    quest: CommonQuestInfo,
    onSave: (CommonQuestInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val questInfoState = remember { QuestInfoState() }

    // Initialize the state with the quest's data
    LaunchedEffect(quest) {
        questInfoState.fromBaseQuest(quest)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Quest") },
        text = {
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                SetBaseQuest(questInfoState = questInfoState)
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedQuest = questInfoState.toBaseQuest(quest.id)
                    onSave(updatedQuest)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
