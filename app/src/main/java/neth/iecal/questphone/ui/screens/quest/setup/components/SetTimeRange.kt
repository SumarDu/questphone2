package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.utils.formatHour

@Composable
fun SetTimeRange(initialTimeRange: QuestInfoState) {
    var showDialog by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(24) } // 24 represents midnight (12 AM next day)

    Button(
        onClick = { showDialog = true }
    ) {
        Text("Perform Between: " + if(startHour == 0 && endHour == 24) "Full Day" else "(${
            formatHour(
                startHour
            )
        } - ${formatHour(endHour)})")
    }

    if (showDialog) {
        TimeRangeDialog(
            initialStartHour = startHour,
            initialEndHour = endHour,
            onDismiss = { showDialog = false },
            onConfirm = { newStart, newEnd ->
                startHour = newStart
                endHour = if (newEnd == 0) 24 else newEnd // Midnight fix
                showDialog = false
                initialTimeRange.initialTimeRange = listOf(startHour,endHour)
            }
        )
    }
}

@Composable
fun TimeRangeDialog(
    initialStartHour: Int,
    initialEndHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var startHour by remember { mutableStateOf(initialStartHour) }
    var endHour by remember { mutableStateOf(initialEndHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(startHour, endHour) }) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select Time Range",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Start Time", fontSize = 16.sp)
                        HourPicker(
                            selectedHour = startHour,
                            availableHours = (0..23).toList(), // Includes 12 AM (0)
                            onHourSelected = {
                                startHour = it
                                if (endHour <= startHour) endHour = startHour + 1
                            }
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("End Time", fontSize = 16.sp)
                        HourPicker(
                            selectedHour = endHour,
                            availableHours = ((startHour + 1)..24).toList(), // Includes 12 AM (24)
                            onHourSelected = { endHour = it }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp
    )
}

@Composable
fun HourPicker(selectedHour: Int, availableHours: List<Int>, onHourSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .height(160.dp)
            .width(90.dp)
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 30.dp)
        ) {
            items(availableHours) { hour ->
                val isSelected = hour == selectedHour
                Text(
                    text = formatHour(hour),
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onHourSelected(hour) }
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}
