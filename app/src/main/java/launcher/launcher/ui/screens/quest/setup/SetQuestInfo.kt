package launcher.launcher.ui.screens.quest.setup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.quest.components.InstructionsList
import launcher.launcher.ui.screens.quest.setup.components.Navigation
import launcher.launcher.ui.screens.quest.setup.components.SelectDaysOfWeek

@Composable
fun EditQuest(
    navController: NavController,
    initialTitle: String = "Study 3hrs",
    initialReward: Int = 10,
    initialInstructions: List<String> = listOf("Revise 0.C", "Practice Maths", "Complete Home-Works")
) {
    var questTitle by remember { mutableStateOf(initialTitle) }
    var reward by remember { mutableStateOf(initialReward) }
    var instructions by remember { mutableStateOf(initialInstructions) }
    var newInstruction by remember { mutableStateOf("") }
    var showAddInstructionDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {

            Navigation(onNextPressed = {
//                navController.navigate(Screen.SetQuestInfo.route)
            },
                onBackPressed = { navController.popBackStack() })
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            horizontalAlignment = Alignment.Start,
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = questTitle,
                    onValueChange = { questTitle = it },
                    label = { Text("Quest Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                )

                OutlinedTextField(
                    value = reward.toString(),
                    onValueChange = {
                        reward = it.toIntOrNull() ?: reward
                    },
                    label = { Text("Reward (coins)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
                SelectDaysOfWeek(onDaysSelected = {})
                InstructionsList(
                    instructions = instructions,
                    onAddInstruction = { showAddInstructionDialog = true },
                    onDeleteInstruction = { instruction ->
                        instructions = instructions.filter { it != instruction }
                    }
                )
            }
        }

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
                                instructions = instructions + newInstruction
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
}