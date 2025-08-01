package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.data.DayOfWeek
import neth.iecal.questphone.data.SchedulingInfo
import neth.iecal.questphone.data.SchedulingType
import neth.iecal.questphone.data.quest.QuestInfoState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedScheduling(
    questInfoState: QuestInfoState,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Schedule Quest",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Choose how you want to schedule this quest.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Scheduling Type Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SchedulingTypeOption(
                title = "Weekly Recurring",
                description = "Repeat on specific days of the week",
                isSelected = questInfoState.schedulingInfo.type == SchedulingType.WEEKLY,
                onClick = {
                    questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                        type = SchedulingType.WEEKLY
                    )
                }
            )

            SchedulingTypeOption(
                title = "Specific Date",
                description = "One-time quest on a specific date",
                isSelected = questInfoState.schedulingInfo.type == SchedulingType.SPECIFIC_DATE,
                onClick = {
                    questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                        type = SchedulingType.SPECIFIC_DATE
                    )
                }
            )

            SchedulingTypeOption(
                title = "Monthly on Date",
                description = "Repeat on the same date each month (e.g., 15th)",
                isSelected = questInfoState.schedulingInfo.type == SchedulingType.MONTHLY_DATE,
                onClick = {
                    questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                        type = SchedulingType.MONTHLY_DATE
                    )
                }
            )

            SchedulingTypeOption(
                title = "Monthly by Day",
                description = "Repeat on a specific day of the week (e.g., first Wednesday)",
                isSelected = questInfoState.schedulingInfo.type == SchedulingType.MONTHLY_BY_DAY,
                onClick = {
                    questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                        type = SchedulingType.MONTHLY_BY_DAY
                    )
                }
            )
        }

        // Configuration based on selected type
        when (questInfoState.schedulingInfo.type) {
            SchedulingType.WEEKLY -> {
                WeeklySchedulingConfig(questInfoState)
            }
            SchedulingType.SPECIFIC_DATE -> {
                SpecificDateConfig(
                    questInfoState = questInfoState,
                    onShowDatePicker = { showDatePicker = true }
                )
            }
            SchedulingType.MONTHLY_DATE -> {
                MonthlyDateConfig(questInfoState)
            }
            SchedulingType.MONTHLY_BY_DAY -> {
                MonthlyByDayConfig(questInfoState)
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { dateMillis ->
                dateMillis?.let {
                    val date = java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                        specificDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    )
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun SchedulingTypeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklySchedulingConfig(questInfoState: QuestInfoState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Select Days",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DayOfWeek.entries.forEach { day ->
                DayButton(
                    day = day,
                    isSelected = day in questInfoState.schedulingInfo.selectedDays,
                    onSelected = { selected ->
                        val newSelectedDays = if (selected) {
                            questInfoState.schedulingInfo.selectedDays + day
                        } else {
                            questInfoState.schedulingInfo.selectedDays - day
                        }
                        questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                            selectedDays = newSelectedDays
                        )
                        // Also update the legacy selectedDays for backward compatibility
                        questInfoState.selectedDays = newSelectedDays
                    }
                )
            }
        }
    }
}

@Composable
private fun SpecificDateConfig(
    questInfoState: QuestInfoState,
    onShowDatePicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Select Date",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        OutlinedButton(
            onClick = onShowDatePicker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Select Date"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = questInfoState.schedulingInfo.specificDate?.let {
                    try {
                        LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                    } catch (e: DateTimeParseException) {
                        "Select Date"
                    }
                } ?: "Select Date"
            )
        }
    }
}

@Composable
private fun MonthlyDateConfig(questInfoState: QuestInfoState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Day of Month",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        OutlinedTextField(
            value = questInfoState.schedulingInfo.monthlyDate?.toString() ?: "",
            onValueChange = { value ->
                val day = value.toIntOrNull()
                if (day != null && day in 1..31) {
                    questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                        monthlyDate = day
                    )
                } else if (value.isEmpty()) {
                    questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                        monthlyDate = null
                    )
                }
            },
            label = { Text("Day (1-31)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text("Quest will repeat on this day each month")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyByDayConfig(questInfoState: QuestInfoState) {
    val weekOptions = mapOf(1 to "First", 2 to "Second", 3 to "Third", 4 to "Fourth", -1 to "Last")
    var weekDropdownExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Week of the month selection
        ExposedDropdownMenuBox(
            expanded = weekDropdownExpanded,
            onExpandedChange = { weekDropdownExpanded = !weekDropdownExpanded }
        ) {
            OutlinedTextField(
                value = weekOptions[questInfoState.schedulingInfo.monthlyWeekInMonth] ?: "Select Week",
                onValueChange = {},
                readOnly = true,
                label = { Text("Week of Month") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = weekDropdownExpanded,
                onDismissRequest = { weekDropdownExpanded = false }
            ) {
                weekOptions.forEach { (weekNum, weekName) ->
                    DropdownMenuItem(
                        text = { Text(weekName) },
                        onClick = {
                            questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                                monthlyWeekInMonth = weekNum
                            )
                            weekDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Day of the week selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DayOfWeek.entries.forEach { day ->
                DayButton(
                    day = day,
                    isSelected = questInfoState.schedulingInfo.monthlyDayOfWeek == day,
                    onSelected = { selected ->
                        questInfoState.schedulingInfo = questInfoState.schedulingInfo.copy(
                            monthlyDayOfWeek = if (selected) day else null
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
