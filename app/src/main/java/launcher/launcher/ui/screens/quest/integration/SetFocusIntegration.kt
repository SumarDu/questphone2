package launcher.launcher.ui.screens.quest.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import launcher.launcher.ui.navigation.Screen

@Composable
fun SetFocusIntegration(
    navController: NavController
) {
    val apps = listOf("Chrome", "YouTube", "WhatsApp", "Spotify", "Telegram")
    var selectedApps by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        floatingActionButton = {
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between buttons
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(text = "Previous")
                }

                Button(
                    onClick = {
                        navController.navigate(Screen.SetFocusIntegration.route)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(text = "Next")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp) // Consistent padding
        ) {
            SetFocusTimeUI()
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Select Unrestricted Apps",
                modifier = Modifier.padding(bottom = 16.dp),
                fontWeight = FontWeight.Bold
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(apps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedApps = if (selectedApps.contains(app)) {
                                    selectedApps - app
                                } else {
                                    selectedApps + app
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedApps.contains(app),
                            onCheckedChange = { isChecked ->
                                selectedApps = if (isChecked) selectedApps + app else selectedApps - app
                            }
                        )
                        Text(
                            app,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

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
