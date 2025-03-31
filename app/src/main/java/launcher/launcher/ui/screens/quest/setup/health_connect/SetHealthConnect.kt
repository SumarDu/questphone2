package launcher.launcher.ui.screens.quest.setup.health_connect

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.BaseQuestState
import launcher.launcher.data.quest.health.HealthQuest
import launcher.launcher.data.quest.health.HealthTaskType
import launcher.launcher.data.quest.health.getUnitForType
import launcher.launcher.ui.screens.quest.setup.ReviewDialog
import launcher.launcher.ui.screens.quest.setup.components.SetBaseQuest
import launcher.launcher.utils.QuestHelper

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetHealthConnect(navController: NavHostController) {
    val scrollState = rememberScrollState()
    val sp = QuestHelper(LocalContext.current)

    val baseQuestState = remember { BaseQuestState(initialIntegrationId = IntegrationId.HEALTH_CONNECT) }
    val healthQuest = remember { mutableStateOf(HealthQuest()) }
    val isReviewDialogVisible = remember { mutableStateOf(false) }

    if (isReviewDialogVisible.value) {
        val baseQuest = baseQuestState.toBaseQuest()
        ReviewDialog(
            items = listOf(
                baseQuest, healthQuest.value
            ),
            onConfirm = {
                sp.saveInstruction(baseQuest.title,baseQuestState.instructions)
                sp.appendToQuestList(
                    baseQuest, healthQuest.value
                )
                isReviewDialogVisible.value = false
                navController.popBackStack()
            },
            onDismiss = {
                isReviewDialogVisible.value = false
            }
        )
    }

    Scaffold() { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = "Health Connect",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    )

                    SetBaseQuest(baseQuestState, isTimeRangeSupported = false)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Health Goal Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Task Type Dropdown
                            HealthTaskTypeSelector(
                                selectedType = healthQuest.value.type,
                                onTypeSelected = { healthQuest.value = healthQuest.value.copy(type = it) }
                            )

                            // Goal Config Inputs
                            GoalConfigInput(
                                label = "Initial Count",
                                value = healthQuest.value.healthGoalConfig.initial.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: 0
                                    healthQuest.value = healthQuest.value.copy(
                                        healthGoalConfig = healthQuest.value.healthGoalConfig.copy(initial = newValue)
                                    )
                                },
                                unit = getUnitForType(healthQuest.value.type)
                            )

                            GoalConfigInput(
                                label = "Increment Daily By",
                                value = healthQuest.value.healthGoalConfig.increment.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: 0
                                    healthQuest.value = healthQuest.value.copy(
                                        healthGoalConfig = healthQuest.value.healthGoalConfig.copy(increment = newValue)
                                    )
                                },
                                unit = getUnitForType(healthQuest.value.type)
                            )
                            GoalConfigInput(
                                label = "Final Count",
                                value = healthQuest.value.healthGoalConfig.final.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: 0
                                    healthQuest.value = healthQuest.value.copy(
                                        healthGoalConfig = healthQuest.value.healthGoalConfig.copy(final = newValue)
                                    )
                                },
                                unit = getUnitForType(healthQuest.value.type)
                            )

                            }

                    Button(
                        onClick = { isReviewDialogVisible.value = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Done"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Quest",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(Modifier.size(100.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTaskTypeSelector(
    selectedType: HealthTaskType,
    onTypeSelected: (HealthTaskType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedType.name.lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Activity Type") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select activity type"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            HealthTaskType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GoalConfigInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) onValueChange(it) },
        label = { Text(label) },
        trailingIcon = {
            Text(
                text = unit,
                modifier = Modifier.padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

