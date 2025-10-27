package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.utils.formatTimeMinutes
import neth.iecal.questphone.utils.toMinutesRange

@Composable
fun SetTimeRange(initialTimeRange: QuestInfoState) {
    // initialize from existing state (supports legacy hours)
    val (initStart, initEnd) = toMinutesRange(initialTimeRange.initialTimeRange)
    var startMinutes by remember { mutableStateOf(initStart) }
    var endMinutes by remember { mutableStateOf(initEnd) }
    var deadlineMinutes by remember { mutableStateOf(initialTimeRange.deadlineMinutes) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showDeadlinePicker by remember { mutableStateOf(false) }

    // Update state when initialTimeRange changes (e.g., when editing a quest)
    LaunchedEffect(initialTimeRange.initialTimeRange, initialTimeRange.deadlineMinutes) {
        val (newStart, newEnd) = toMinutesRange(initialTimeRange.initialTimeRange)
        startMinutes = newStart
        endMinutes = newEnd
        deadlineMinutes = initialTimeRange.deadlineMinutes
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Start time selector
            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Start time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text(formatTimeMinutes(startMinutes), style = MaterialTheme.typography.bodyLarge)
                }
            }
            // End time selector
            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("End time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text(formatTimeMinutes(endMinutes), style = MaterialTheme.typography.bodyLarge)
                }
            }
            // Deadline selector
            OutlinedButton(
                onClick = { showDeadlinePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Deadline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    val d = deadlineMinutes
                    val label = if (d <= 0) "—" else formatTimeMinutes(d)
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    if (showStartPicker) {
        SingleTimePickerDialog(
            title = "Select Start Time",
            initialMinutes = startMinutes,
            onDismiss = { showStartPicker = false },
            onConfirm = { minutes ->
                startMinutes = minutes.coerceIn(0, 1435)
                if (endMinutes <= startMinutes) endMinutes = (startMinutes + 1).coerceAtMost(1440)
                initialTimeRange.initialTimeRange = listOf(startMinutes, endMinutes)
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        SingleTimePickerDialog(
            title = "Select End Time",
            initialMinutes = endMinutes,
            onDismiss = { showEndPicker = false },
            onConfirm = { minutes ->
                val m = minutes.coerceIn(0, 1440)
                endMinutes = if (m <= startMinutes) (startMinutes + 1).coerceAtMost(1440) else m
                initialTimeRange.initialTimeRange = listOf(startMinutes, endMinutes)
                showEndPicker = false
            }
        )
    }

    if (showDeadlinePicker) {
        DeadlineTimePickerDialog(
            initialMinutes = deadlineMinutes.coerceAtLeast(0),
            onClear = {
                deadlineMinutes = 0
                initialTimeRange.deadlineMinutes = 0
                showDeadlinePicker = false
            },
            onDismiss = { showDeadlinePicker = false },
            onConfirm = { minutes ->
                val m = minutes.coerceIn(0, 1439)
                deadlineMinutes = m
                initialTimeRange.deadlineMinutes = m
                showDeadlinePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeDialog(
    initialStartMinutes: Int,
    initialEndMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var startH by remember { mutableStateOf((initialStartMinutes / 60).coerceIn(0, 23)) }
    var startM by remember { mutableStateOf((initialStartMinutes % 60).coerceIn(0, 59)) }
    var endH by remember { mutableStateOf(((initialEndMinutes.coerceAtMost(1439)) / 60).coerceIn(0, 23)) }
    var endM by remember { mutableStateOf(((initialEndMinutes.coerceAtMost(1440)) % 60).coerceIn(0, 59)) }
    var isAllDay by remember { mutableStateOf(initialStartMinutes == 0 && initialEndMinutes == 1440) }

    fun startTotal() = startH * 60 + startM
    fun endTotal(): Int {
        return if (isAllDay) 1440 else (endH * 60 + endM)
    }


    val startState = rememberTimePickerState(
        initialHour = startH,
        initialMinute = startM,
        is24Hour = false
    )
    val endState = rememberTimePickerState(
        initialHour = endH,
        initialMinute = endM,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val sHour = startState.hour
                    val eHour = endState.hour
                    val s = if (isAllDay) 0 else (sHour * 60 + startState.minute)
                    val rawE = if (isAllDay) 1440 else (eHour * 60 + endState.minute)
                    val e = if (isAllDay) 1440 else rawE.let { if (it <= s) (s + 5).coerceAtMost(1440) else it }
                    onConfirm(s, e)
                }
            ) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select Time Range",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("All day", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 420.dp
                    if (isNarrow) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Start", fontSize = 16.sp)
                                if (!isAllDay) {
                                    TimePicker(state = startState)
                                } else {
                                    Text("${formatTimeMinutes(0)}", fontSize = 18.sp)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("End", fontSize = 16.sp)
                                if (!isAllDay) {
                                    TimePicker(state = endState)
                                } else {
                                    Text("${formatTimeMinutes(1440)}", fontSize = 18.sp)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Start", fontSize = 16.sp)
                                if (!isAllDay) {
                                    TimePicker(state = startState)
                                } else {
                                    Text("${formatTimeMinutes(0)}", fontSize = 18.sp)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("End", fontSize = 16.sp)
                                if (!isAllDay) {
                                    TimePicker(state = endState)
                                } else {
                                    Text("${formatTimeMinutes(1440)}", fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlineTimePickerDialog(
    initialMinutes: Int,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val init = initialMinutes.coerceIn(0, 1439)
    val state = rememberTimePickerState(
        initialHour = init / 60,
        initialMinute = init % 60,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Deadline", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                TimePicker(state = state)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleTimePickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val initHour = (initialMinutes.coerceAtLeast(0).coerceAtMost(1439)) / 60
    val initMinute = (initialMinutes % 60)
    val state = rememberTimePickerState(initialHour = initHour, initialMinute = initMinute, is24Hour = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                TimePicker(state = state)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp
    )
}

@Composable
private fun TimePickerColumn(
    hour: Int,
    minute: Int,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SelectorPicker(
            values = (0..23).toList(),
            selected = hour,
            labelProvider = { h -> if (h == 0) "12 AM" else if (h < 12) "$h AM" else if (h == 12) "12 PM" else "${h - 12} PM" },
            onSelect = onHour,
            width = 100.dp
        )
        SelectorPicker(
            values = (0..55 step 5).toList(),
            selected = minute,
            labelProvider = { m -> String.format("%02d", m) },
            onSelect = onMinute,
            width = 64.dp
        )
    }
}

@Composable
private fun <T> SelectorPicker(
    values: List<T>,
    selected: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit,
    width: Dp = 90.dp
) {
    Box(
        modifier = Modifier
            .height(160.dp)
            .width(width)
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 30.dp)
        ) {
            items(values) { value ->
                val isSelected = value == selected
                Text(
                    text = labelProvider(value),
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onSelect(value) }
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}
