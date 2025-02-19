package launcher.launcher.ui.screens.quest.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import launcher.launcher.models.quest.FocusTimeConfig

@Composable
fun SetFocusTimeUI(
    focusTime: MutableState<FocusTimeConfig>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TimeInputRow(
                    label = "Initial Focus Time",
                    time = focusTime.value.initialTime,
                    unit = focusTime.value.initialUnit,
                    onUpdate = { value, unit ->
                        val initial = convertToMinutes(value, unit)
                        val goal = convertToMinutes(focusTime.value.finalTime, focusTime.value.finalUnit)
                        if (initial in 1..goal) {
                            focusTime.value = focusTime.value.copy(initialTime = value, initialUnit = unit)
                        }
                    }
                )
            }
            item {
                TimeInputRow(
                    label = "Increment Daily by",
                    time = focusTime.value.incrementTime,
                    unit = focusTime.value.incrementUnit,
                    availableUnits = listOf("m"),
                    onUpdate = { value, unit ->
                        focusTime.value = focusTime.value.copy(incrementTime = value, incrementUnit = unit)
                    }
                )
            }
            item {
                TimeInputRow(
                    label = "Goal Focus Time",
                    time = focusTime.value.finalTime,
                    unit = focusTime.value.finalUnit,
                    onUpdate = { value, unit ->
                        val initial = convertToMinutes(focusTime.value.initialTime, focusTime.value.initialUnit)
                        val goal = convertToMinutes(value, unit)
                        if (goal >= initial) {
                            focusTime.value = focusTime.value.copy(finalTime = value, finalUnit = unit)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TimeInputRow(label: String, time: String, unit: String, onUpdate: (String, String) -> Unit,availableUnits:List<String> = listOf("h","m"),) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        BasicTextField(
            value = time,
            onValueChange = { if (it.length <= 3) onUpdate(it, unit) },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .width(50.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 6.dp)
        )
        TimeUnitSelector(
            selectedUnit = unit,
            onSelect = { onUpdate(time, it) },
            modifier = Modifier.width(80.dp),
            units = availableUnits
        )
    }
}

@Composable
fun TimeUnitSelector(
    selectedUnit: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    units:List<String>
) {

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        units.forEach { unit ->
            TextButton(
                onClick = { onSelect(unit) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (selectedUnit == unit)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    containerColor = if (selectedUnit == unit)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedUnit == unit) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Converts time to minutes based on the unit ("h" or "m").
 */
fun convertToMinutes(value: String, unit: String): Int {
    val time = value.toIntOrNull() ?: return 0
    return if (unit == "h") time * 60 else time
}
