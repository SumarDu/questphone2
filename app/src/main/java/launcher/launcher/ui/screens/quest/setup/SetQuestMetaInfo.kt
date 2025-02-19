package launcher.launcher.ui.screens.quest.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import launcher.launcher.Constants
import launcher.launcher.models.DayOfWeek
import launcher.launcher.ui.navigation.AddNewQuestSubScreens
import launcher.launcher.ui.screens.quest.components.InstructionsList
import launcher.launcher.ui.screens.quest.setup.components.SelectDaysOfWeek

@Composable
fun SetQuestMetaInfo(
    previousScreen: MutableState<String>,
    nextScreen: MutableState<String>,
    isBackButtonFinish: MutableState<Boolean>,

    isNextEnabled: MutableState<Boolean>,

    instructions: MutableState<List<String>>,
    reward: MutableIntState,
    questTitle: MutableState<String>,


    selectedDays: MutableState<Set<DayOfWeek>>,
    selectedIntegration: MutableState<Int?>,
    ) {
    var newInstruction by remember { mutableStateOf("") }
    var showAddInstructionDialog by remember { mutableStateOf(false) }

    previousScreen.value = AddNewQuestSubScreens.Integration.route
    isNextEnabled.value = questTitle.value.isNotEmpty() && selectedDays.value.isNotEmpty() && reward.intValue!=0

    when(selectedIntegration.value){
        Constants.INTEGRATION_ID_FOCUS -> nextScreen.value = AddNewQuestSubScreens.FocusIntegration.route
        Constants.INTEGRATION_ID_APP_FOCUS -> nextScreen.value = AddNewQuestSubScreens.AppFocusIntegration.route
    }
    isBackButtonFinish.value = false

    OutlinedTextField(
        value = questTitle.value,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = { questTitle.value = it },
        label = { Text("Quest Title") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
    )

    OutlinedTextField(
        value = reward.intValue.toString(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        onValueChange = {
            reward.intValue = it.toIntOrNull() ?: reward.intValue
        },
        label = { Text("Reward (coins)") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    )
    SelectDaysOfWeek(selectedDays)
    InstructionsList(
        instructions = instructions.value,
        onAddInstruction = { showAddInstructionDialog = true },
        onDeleteInstruction = { instruction ->
            instructions.value = instructions.value.filter { it != instruction }
        }
    )

    if (showAddInstructionDialog) {
        AlertDialog(
            onDismissRequest = { showAddInstructionDialog = false },
            title = { Text("Add Instruction") },
            text = {
                OutlinedTextField(
                    value = newInstruction,
                    onValueChange = { newInstruction = it },
                    label = { Text("New Instruction") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newInstruction.isNotBlank()) {
                            instructions.value += newInstruction
                            newInstruction = ""
                        }
                        showAddInstructionDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddInstructionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}