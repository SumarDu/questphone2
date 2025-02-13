package launcher.launcher.ui.screens.quest.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun SetFocusTimeUI() {
    var initialTime by remember { mutableStateOf("1") }
    var finalTime by remember { mutableStateOf("5") }
    var incrementTime by remember { mutableStateOf("15") }

    var initialUnit by remember { mutableStateOf("h") }
    var finalUnit by remember { mutableStateOf("h") }
    var incrementUnit by remember { mutableStateOf("m") }

    val units = listOf("h", "m")

    @Composable
    fun TimeUnitSelector(
        selectedUnit: String,
        onSelect: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Consistent vertical padding
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            Triple("Initial Focus Time", initialTime, initialUnit) to { time: String, unit: String ->
                initialTime = time
                initialUnit = unit
            },
            Triple("Increment Daily by", incrementTime, incrementUnit) to { time: String, unit: String ->
                incrementTime = time
                incrementUnit = unit
            },
            Triple("Goal Focus Time", finalTime, finalUnit) to { time: String, unit: String ->
                finalTime = time
                finalUnit = unit
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