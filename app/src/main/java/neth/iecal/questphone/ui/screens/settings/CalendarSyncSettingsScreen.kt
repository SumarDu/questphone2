package neth.iecal.questphone.ui.screens.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.calendar.CalendarManager
import neth.iecal.questphone.data.calendar.DeviceCalendar
import neth.iecal.questphone.data.settings.CalendarSyncSettings
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.workers.CalendarSyncWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSyncSettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState()
    val calendarSyncSettings = settings.calendarSyncSettings
    
    val calendarManager = remember { CalendarManager(context) }
    var availableCalendars by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    var hasCalendarPermission by remember { mutableStateOf(calendarManager.hasCalendarPermissions()) }
    var isLoading by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCalendarPermission = permissions[Manifest.permission.READ_CALENDAR] == true
        if (hasCalendarPermission) {
            scope.launch {
                availableCalendars = calendarManager.getAvailableCalendars()
            }
        }
    }
    
    // Load available calendars when permission is granted
    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission) {
            availableCalendars = calendarManager.getAvailableCalendars()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar Sync") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                syncStatus = "Syncing..."
                                CalendarSyncWorker.triggerImmediateSync(context)
                                syncStatus = "Sync triggered"
                                isLoading = false
                            }
                        },
                        enabled = hasCalendarPermission && calendarSyncSettings.isEnabled
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Now")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Permission Section
            if (!hasCalendarPermission) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Calendar Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "To sync calendar events with Swift Mark quests, please grant calendar access permission.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
            
            // Main Settings Section
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Calendar Sync Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Enable/Disable Calendar Sync
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Calendar Sync")
                            Text(
                                text = "Automatically create Swift Mark quests from calendar events",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = calendarSyncSettings.isEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    val newSettings = calendarSyncSettings.copy(isEnabled = enabled)
                                    settingsRepository.updateCalendarSyncSettings(newSettings)
                                    
                                    if (enabled) {
                                        CalendarSyncWorker.schedulePeriodicSync(context, newSettings.syncIntervalHours)
                                    } else {
                                        CalendarSyncWorker.cancelPeriodicSync(context)
                                    }
                                }
                            },
                            enabled = hasCalendarPermission
                        )
                    }
                    
                    if (calendarSyncSettings.isEnabled && hasCalendarPermission) {
                        Divider()
                        
                        // Sync Interval
                        Text("Sync Interval")
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = CalendarSyncSettings.SYNC_INTERVALS[calendarSyncSettings.syncIntervalHours] ?: "Custom",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                CalendarSyncSettings.SYNC_INTERVALS.forEach { (hours, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            scope.launch {
                                                val newSettings = calendarSyncSettings.copy(syncIntervalHours = hours)
                                                settingsRepository.updateCalendarSyncSettings(newSettings)
                                                CalendarSyncWorker.schedulePeriodicSync(context, hours)
                                            }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Divider()
                        
                        // Calendar Selection Section
                        Text("Select Calendars")
                        Text(
                            text = if (calendarSyncSettings.selectedCalendarIds.isEmpty()) {
                                "All calendars (${availableCalendars.size} available)"
                            } else {
                                "${calendarSyncSettings.selectedCalendarIds.size} of ${availableCalendars.size} calendars selected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(availableCalendars) { calendar ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                val newSelectedIds = if (calendarSyncSettings.selectedCalendarIds.contains(calendar.id)) {
                                                    calendarSyncSettings.selectedCalendarIds - calendar.id
                                                } else {
                                                    calendarSyncSettings.selectedCalendarIds + calendar.id
                                                }
                                                val newSettings = calendarSyncSettings.copy(selectedCalendarIds = newSelectedIds)
                                                settingsRepository.updateCalendarSyncSettings(newSettings)
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = calendarSyncSettings.selectedCalendarIds.isEmpty() || 
                                                calendarSyncSettings.selectedCalendarIds.contains(calendar.id),
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                val newSelectedIds = if (checked) {
                                                    calendarSyncSettings.selectedCalendarIds + calendar.id
                                                } else {
                                                    calendarSyncSettings.selectedCalendarIds - calendar.id
                                                }
                                                val newSettings = calendarSyncSettings.copy(selectedCalendarIds = newSelectedIds)
                                                settingsRepository.updateCalendarSyncSettings(newSettings)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = calendar.displayName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = calendar.accountName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (availableCalendars.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val newSettings = calendarSyncSettings.copy(selectedCalendarIds = emptySet())
                                            settingsRepository.updateCalendarSyncSettings(newSettings)
                                        }
                                    }
                                ) {
                                    Text("Select All")
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val allIds = availableCalendars.map { it.id }.toSet()
                                            val newSettings = calendarSyncSettings.copy(selectedCalendarIds = allIds)
                                            settingsRepository.updateCalendarSyncSettings(newSettings)
                                        }
                                    }
                                ) {
                                    Text("Select None")
                                }
                            }
                        }
                        
                        Divider()
                        
                        // Default Values Section
                        Text("Default Quest Values")
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = calendarSyncSettings.defaultReward.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { reward ->
                                        scope.launch {
                                            settingsRepository.updateCalendarSyncSettings(
                                                calendarSyncSettings.copy(defaultReward = reward)
                                            )
                                        }
                                    }
                                },
                                label = { Text("Reward") },
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedTextField(
                                value = calendarSyncSettings.defaultDurationMinutes.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { duration ->
                                        scope.launch {
                                            settingsRepository.updateCalendarSyncSettings(
                                                calendarSyncSettings.copy(defaultDurationMinutes = duration)
                                            )
                                        }
                                    }
                                },
                                label = { Text("Duration (min)") },
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedTextField(
                                value = calendarSyncSettings.defaultBreakMinutes.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { breakTime ->
                                        scope.launch {
                                            settingsRepository.updateCalendarSyncSettings(
                                                calendarSyncSettings.copy(defaultBreakMinutes = breakTime)
                                            )
                                        }
                                    }
                                },
                                label = { Text("Break (min)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Divider()
                        
                        // Manual Sync Button
                        Button(
                            onClick = {
                                scope.launch {
                                    syncStatus = "Syncing..."
                                    CalendarSyncWorker.triggerImmediateSync(context)
                                    // Wait a bit and then update status
                                    kotlinx.coroutines.delay(2000)
                                    syncStatus = "Manual sync completed"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasCalendarPermission && calendarSyncSettings.isEnabled
                        ) {
                            Text("Sync Now")
                        }
                    }
                }
            }
            
            // Status Section
            if (syncStatus != null) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sync Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(syncStatus!!)
                        
                        if (calendarSyncSettings.lastSyncTimestamp > 0) {
                            Text(
                                text = "Last sync: ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm").format(java.util.Date(calendarSyncSettings.lastSyncTimestamp))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Instructions Section
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How to Use",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Add events to your device calendar\n" +
                                "• Use 'C: n' in event description for reward (e.g., 'C: 10' for 10 coins)\n" +
                                "• Use 'D: y' for quest duration in minutes (e.g., 'D: 30' for 30 minutes)\n" +
                                "• Use 'B: x' for break duration in minutes (e.g., 'B: 5' for 5 minutes)\n" +
                                "• Calendar events will automatically create Swift Mark quests",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
