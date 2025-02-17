package launcher.launcher.ui.screens.quest.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    focusTimeState : MutableState<FocusTimeConfig>
) {
    val focusTime = focusTimeState.value
    val units = listOf("h", "m")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Consistent padding
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            Triple("Initial Focus Time", focusTime.initialTime, focusTime.initialUnit) to { time: String, unit: String ->
                focusTimeState.value = focusTime.copy(initialTime = time, initialUnit = unit)
            },
            Triple("Increment Daily by", focusTime.incrementTime, focusTime.incrementUnit) to { time: String, unit: String ->
                focusTimeState.value = focusTime.copy(incrementTime = time, incrementUnit = unit)
            },
            Triple("Goal Focus Time", focusTime.finalTime, focusTime.finalUnit) to { time: String, unit: String ->
                focusTimeState.value = focusTime.copy(finalTime = time, finalUnit = unit)
            }
        ).forEach { (data, update) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .padding(vertical = 8.dp), // Unified vertical padding
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Consistent spacing
            ) {
                Text(
                    text = data.first,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                BasicTextField(
                    value = data.second,
                    onValueChange = { if (it.length <= 3) update(it, data.third) },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .width(50.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 6.dp) // Consistent padding
                )
                TimeUnitSelector(
                    selectedUnit = data.third,
                    onSelect = { update(data.second, it) },
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }
}

@Composable
fun TimeUnitSelector(
    selectedUnit: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val units = listOf("h", "m")

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp) // Consistent padding
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
