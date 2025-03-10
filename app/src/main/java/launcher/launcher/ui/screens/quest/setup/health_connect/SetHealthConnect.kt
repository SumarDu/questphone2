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
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.BaseQuestState
import launcher.launcher.data.quest.health.HealthGoal
import launcher.launcher.data.quest.health.HealthTaskType
import launcher.launcher.ui.screens.quest.setup.ReviewDialog
import launcher.launcher.ui.screens.quest.setup.components.SetBaseQuest
import launcher.launcher.utils.QuestHelper

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetHealthConnect() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val sp = QuestHelper(LocalContext.current)

    val baseQuestState = remember { BaseQuestState(initialIntegrationId = IntegrationId.HEALTH_CONNECT) }
    val healthGoal = remember { mutableStateOf(HealthGoal()) }
    val isReviewDialogVisible = remember { mutableStateOf(false) }

    if (isReviewDialogVisible.value) {
        val baseQuest = baseQuestState.toBaseQuest()
        ReviewDialog(
            items = listOf(
                baseQuest, healthGoal.value
            ),
            onConfirm = {
                sp.appendToQuestList(
                    baseQuest, healthGoal.value
                )
                isReviewDialogVisible.value = false
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

                    SetBaseQuest(baseQuestState)

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
                            ElegantHealthTaskTypeSelector(
                                selectedType = healthGoal.value.type,
                                onTypeSelected = { healthGoal.value = healthGoal.value.copy(type = it) }
                            )

                            // Goal Config Inputs
                            ElegantGoalConfigInput(
                                label = "Initial Count",
                                value = healthGoal.value.healthGoalConfig.initial.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: 0
                                    healthGoal.value = healthGoal.value.copy(
                                        healthGoalConfig = healthGoal.value.healthGoalConfig.copy(initial = newValue)
                                    )
                                },
                                unit = getUnitForType(healthGoal.value.type)
                            )

                            ElegantGoalConfigInput(
                                label = "Increment Daily By",
                                value = healthGoal.value.healthGoalConfig.increment.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: 0
                                    healthGoal.value = healthGoal.value.copy(
                                        healthGoalConfig = healthGoal.value.healthGoalConfig.copy(increment = newValue)
                                    )
                                },
                                unit = getUnitForType(healthGoal.value.type)
                            )
                            ElegantGoalConfigInput(
                                label = "Final Count",
                                value = healthGoal.value.healthGoalConfig.final.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: 0
                                    healthGoal.value = healthGoal.value.copy(
                                        healthGoalConfig = healthGoal.value.healthGoalConfig.copy(final = newValue)
                                    )
                                },
                                unit = getUnitForType(healthGoal.value.type)
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
fun ElegantHealthTaskTypeSelector(
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
fun ElegantGoalConfigInput(
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

// Unit mapping for display
private fun getUnitForType(type: HealthTaskType): String = when (type) {
    HealthTaskType.STEPS -> "steps"
    HealthTaskType.CALORIES -> "cal"
    HealthTaskType.DISTANCE -> "km"
    HealthTaskType.SLEEP -> "hrs"
    HealthTaskType.WATER_INTAKE -> "l"
}