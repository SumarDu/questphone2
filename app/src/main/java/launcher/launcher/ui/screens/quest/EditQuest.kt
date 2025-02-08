package launcher.launcher.ui.screens.quest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EditQuest(
    onNavigateToQuestTracker: () -> Unit,
    coins: Int = 100,
    initialTitle: String = "Study 3hrs",
    initialReward: Int = 10,
    initialInstructions: List<String> = listOf("Revise 0.C", "Practice Maths", "Complete Home-Works"),
    onSaveQuest: ((String, Int, List<String>) -> Unit)? = null
) {
    var questTitle by remember { mutableStateOf(initialTitle) }
    var reward by remember { mutableStateOf(initialReward) }
    var instructions by remember { mutableStateOf(initialInstructions) }
    var newInstruction by remember { mutableStateOf("") }
    var showAddInstructionDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Button(
                onClick = {
                    if (onSaveQuest != null) {
                        onSaveQuest(questTitle, reward, instructions)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(text = "Save Quest")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "$coins coins",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.End)
            )

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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    IconButton(onClick = { showAddInstructionDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add instruction")
                    }
                }

                LazyColumn {
                    items(instructions) { instruction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\u2022 $instruction",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    instructions = instructions.filter { it != instruction }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete instruction",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
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