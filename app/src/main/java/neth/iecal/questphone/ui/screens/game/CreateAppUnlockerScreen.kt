package neth.iecal.questphone.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import neth.iecal.questphone.data.quest.QuestDatabaseProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAppUnlockerScreen(
    navController: NavHostController,
    viewModel: CreateAppUnlockerViewModel = viewModel(
        factory = CreateAppUnlockerViewModelFactory(
            QuestDatabaseProvider.getInstance(LocalContext.current).appUnlockerItemDao()
        )
    )
) {
    val context = LocalContext.current
    var showAppSelector by remember { mutableStateOf(false) }
    var selectedTimePreset by remember { mutableStateOf<TimePreset?>(null) }
    var customTime by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showPurchaseTimeRangeDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadApps(context)
    }

    val scrollState = rememberScrollState()

    // System bar padding to prevent title overlap
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title with proper spacing
        Text(
            text = "Create New App Unlocker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Modern App Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAppSelector = !showAppSelector }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Application",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.selectedApp?.name ?: "Select an application",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (viewModel.selectedApp != null) 
                                MaterialTheme.colorScheme.onSurface 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (showAppSelector) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (showAppSelector) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search for an app") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )
                    
                    val filteredApps = viewModel.apps.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            filteredApps.forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectedApp = app
                                            showAppSelector = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (viewModel.selectedApp?.packageName == app.packageName) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Price Input
        OutlinedTextField(
            value = viewModel.price,
            onValueChange = { viewModel.price = it },
            label = { Text("Price (Coins)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Modern Time Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Unlock Duration",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Time Presets
                val timePresets = listOf(
                    TimePreset("15 min", 0, 15),
                    TimePreset("30 min", 0, 30),
                    TimePreset("1 hour", 1, 0),
                    TimePreset("2 hours", 2, 0),
                    TimePreset("Custom", -1, -1)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timePresets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedTimePreset = preset
                                    customTime = preset.label == "Custom"
                                    if (!customTime) {
                                        viewModel.hours = preset.hours.toString()
                                        viewModel.minutes = preset.minutes.toString()
                                    }
                                }
                                .background(
                                    if (selectedTimePreset == preset)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTimePreset == preset,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Custom time input
                if (customTime) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.hours,
                            onValueChange = { viewModel.hours = it },
                            label = { Text("Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = viewModel.minutes,
                            onValueChange = { viewModel.minutes = it },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Purchase Time Restriction (Optional)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Purchase Time Restriction",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Switch(
                        checked = viewModel.enableTimeRestriction,
                        onCheckedChange = { viewModel.enableTimeRestriction = it }
                    )
                }
                
                if (viewModel.enableTimeRestriction) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This unlocker can only be purchased during the specified time range.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPurchaseTimeRangeDialog = true }
                    ) {
                        OutlinedTextField(
                            value = "${neth.iecal.questphone.utils.formatTimeMinutes(viewModel.purchaseStartTimeMinutes)} — ${neth.iecal.questphone.utils.formatTimeMinutes(viewModel.purchaseEndTimeMinutes)}",
                            onValueChange = {},
                            label = { Text("Purchase time range") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = {
                viewModel.saveAppUnlocker {
                    navController.popBackStack()
                }
            },
            enabled = viewModel.selectedApp != null && 
                     viewModel.price.isNotBlank() && 
                     (viewModel.hours.isNotBlank() || viewModel.minutes.isNotBlank()),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Save App Unlocker",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
    
    // Purchase Time Range Dialog
    if (showPurchaseTimeRangeDialog) {
        neth.iecal.questphone.ui.screens.quest.setup.components.TimeRangeDialog(
            initialStartMinutes = viewModel.purchaseStartTimeMinutes,
            initialEndMinutes = viewModel.purchaseEndTimeMinutes,
            onDismiss = { showPurchaseTimeRangeDialog = false },
            onConfirm = { s, e ->
                viewModel.purchaseStartTimeMinutes = s
                viewModel.purchaseEndTimeMinutes = e
                showPurchaseTimeRangeDialog = false
            }
        )
    }
}

data class TimePreset(
    val label: String,
    val hours: Int,
    val minutes: Int
)
