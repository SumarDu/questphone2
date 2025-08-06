package neth.iecal.questphone.ui.screens.launcher.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import neth.iecal.questphone.data.timer.TimerMode
import android.util.Log
import neth.iecal.questphone.data.timer.TimerState
import neth.iecal.questphone.ui.screens.launcher.TimerViewModel
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.utils.getCurrentDate

@Composable
fun LiveTimer(
    modifier: Modifier = Modifier,
    timerViewModel: TimerViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState()
    
    val timerText by timerViewModel.timerText.collectAsState()
    val timerMode by timerViewModel.timerMode.collectAsState()
    val timerState: TimerState by timerViewModel.timerState.collectAsState()
    var showDialog by remember(timerMode) { mutableStateOf(false) }
    var unplannedBreakReason by remember { mutableStateOf("") }
    var showAddTimeDialog by remember(timerMode) { mutableStateOf(false) }
    var showQuestListDialog by remember(timerMode) { mutableStateOf(false) }
    
    // Helper functions to categorize quests
    fun isRepeatingQuest(quest: CommonQuestInfo): Boolean {
        return quest.selected_days.isNotEmpty()
    }
    
    fun isClonedQuest(quest: CommonQuestInfo): Boolean {
        return quest.title.contains("[C]") || quest.auto_destruct == getCurrentDate()
    }
    
    fun isOneTimeQuest(quest: CommonQuestInfo): Boolean {
        return !isRepeatingQuest(quest) && !isClonedQuest(quest)
    }

    val timerColor = when (timerMode) {
        TimerMode.QUEST_COUNTDOWN -> MaterialTheme.colorScheme.primary
        TimerMode.OVERTIME -> if (timerState.isBreakOvertime) MaterialTheme.colorScheme.error else Color(0xFFDC143C)
        TimerMode.BREAK -> Color.Green // Example color
        TimerMode.UNPLANNED_BREAK -> Color.Gray
        TimerMode.INACTIVE -> MaterialTheme.colorScheme.onSurface
        TimerMode.INFO -> Color(0xFF8A2BE2) // Purple
        TimerMode.UNLOCK -> Color.Yellow
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Unplanned Break") },
            text = {
                Column {
                    Text("Select or enter a reason for the unplanned break:")
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = unplannedBreakReason,
                        onValueChange = { unplannedBreakReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (settings.unplannedBreakReasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Or choose from your list:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.height(150.dp)) {
                            items(settings.unplannedBreakReasons) { reason ->
                                Text(
                                    text = reason,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { unplannedBreakReason = reason }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        timerViewModel.startUnplannedBreak(unplannedBreakReason)
                        showDialog = false
                        unplannedBreakReason = ""
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        IconButton(
            onClick = { timerViewModel.toggleInfoMode() },
            enabled = timerMode == TimerMode.QUEST_COUNTDOWN || timerMode == TimerMode.BREAK || timerMode == TimerMode.INFO
        ) {
            Icon(Icons.Default.Info, contentDescription = "Info")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = timerText,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = timerColor,
            modifier = Modifier.clickable(enabled = timerMode != TimerMode.INFO) {
                Log.d("LiveTimer", "Timer clicked. Mode: $timerMode")
                if (timerMode == TimerMode.UNPLANNED_BREAK) {
                    Log.d("LiveTimer", "Stopping unplanned break.")
                    timerViewModel.stopUnplannedBreak()
                } else if (timerMode == TimerMode.BREAK) {
                    Log.d("LiveTimer", "Ending break early.")
                    timerViewModel.endBreakEarly()
                } else if (timerMode != TimerMode.OVERTIME && timerMode != TimerMode.UNLOCK) {
                    Log.d("LiveTimer", "Showing unplanned break dialog.")
                    showDialog = true
                }
            }
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = {
                if (timerMode == TimerMode.BREAK || (timerMode == TimerMode.OVERTIME && timerState.isBreakOvertime)) {
                    showQuestListDialog = true
                } else {
                    showAddTimeDialog = true
                }
            },
            enabled = timerMode != TimerMode.UNPLANNED_BREAK && !(timerMode == TimerMode.OVERTIME && !timerState.isBreakOvertime) && timerMode != TimerMode.INFO && !timerState.isDeepFocusLocking
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }

    if (showAddTimeDialog) {
        AlertDialog(
            onDismissRequest = { showAddTimeDialog = false },
            title = { Text("Add Time") },
            text = { Text("Select how much time to add to the current quest.") },
            confirmButton = {
                Row {
                    Button(onClick = { timerViewModel.addTime(5); showAddTimeDialog = false }) {
                        Text("5m")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { timerViewModel.addTime(10); showAddTimeDialog = false }) {
                        Text("10m")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { timerViewModel.addTime(15); showAddTimeDialog = false }) {
                        Text("15m")
                    }
                }
            },
            dismissButton = {
                Button(onClick = { showAddTimeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQuestListDialog) {
        val allQuests by timerViewModel.allQuests.collectAsState(initial = emptyList())
        
        // Filter quests based on settings
        val filteredQuests = allQuests.filter { quest ->
            when {
                isRepeatingQuest(quest) -> settings.showRepeatingQuestsInDialog
                isClonedQuest(quest) -> settings.showClonedQuestsInDialog
                isOneTimeQuest(quest) -> settings.showOneTimeQuestsInDialog
                else -> true // Show by default if categorization fails
            }
        }

        AlertDialog(
            onDismissRequest = { showQuestListDialog = false },
            title = { Text("Select a Quest") },
            text = {
                if (filteredQuests.isEmpty()) {
                    Text(
                        text = "No quests available. Check your filter settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn {
                        items(filteredQuests) { quest ->
                            Text(
                                text = quest.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = timerMode != TimerMode.INFO) { 
                                        timerViewModel.cloneAndStartQuest(quest)
                                        showQuestListDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showQuestListDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
