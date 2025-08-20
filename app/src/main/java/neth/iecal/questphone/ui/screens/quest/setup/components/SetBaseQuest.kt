package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.OutlinedButton
import kotlinx.coroutines.flow.first
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.data.quest.QuestPriority
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.getCurrentDay
import neth.iecal.questphone.utils.formatTimeMinutes
import neth.iecal.questphone.data.game.AppUnlockerItem

@Composable
private fun PriorityOption(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 12.dp)
            .clickable { onClick() }
    ) {
        val size = 20.dp
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .height(size)
                .width(size)
                .background(color, shape = MaterialTheme.shapes.small)
                .border(
                    width = if (selected) 2.dp else Dp.Hairline,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBaseQuest(questInfoState: QuestInfoState, isTimeRangeSupported: Boolean = true) {

    val allQuestTitles = mutableSetOf<String>()

    var isTitleDublicate by remember { mutableStateOf(false) }
    val dao = QuestDatabaseProvider.getInstance(LocalContext.current).questDao()

    LaunchedEffect(Unit) {
        allQuestTitles.addAll(
            dao.getAllQuests().first().map { it.title }
        )

    }

    OutlinedTextField(
        value = questInfoState.title,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = {
            isTitleDublicate = allQuestTitles.contains(it)
            questInfoState.title = it
                        },
        label = { Text("Quest Title") },
        modifier = Modifier.fillMaxWidth(),
        isError = isTitleDublicate
    )
    if(isTitleDublicate){
        Text(text = "Title already exists", color = MaterialTheme.colorScheme.error)
    }

    if(questInfoState.selectedDays.contains(getCurrentDay()) && User.userInfo.getCreatedOnString() != getCurrentDate()){
        Text("To prevent abuse, you can't perform this quest today. You'll be able to do it starting from the next time it occurs.")
    }
    EnhancedScheduling(questInfoState)


    OutlinedTextField(
        value = questInfoState.instructions,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = { questInfoState.instructions = it }, // Direct update
        label = { Text("Instructions") },
        modifier = Modifier.fillMaxWidth()
            .height(200.dp)
    )
    AutoDestruct(questInfoState)

    if(isTimeRangeSupported){
        SetTimeRange(questInfoState)
    }

    // Priority selector (sorted: Red, Green, Beige, Blue, Gray) - vertical compact list
    Text("Priority", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        PriorityOption(
            label = "Important & Urgent",
            color = Color(0xFFEF4444),
            selected = questInfoState.priority == QuestPriority.IMPORTANT_URGENT
        ) { questInfoState.priority = QuestPriority.IMPORTANT_URGENT }
        Spacer(modifier = Modifier.height(6.dp))
        PriorityOption(
            label = "Important, Not Urgent",
            color = Color(0xFF10B981),
            selected = questInfoState.priority == QuestPriority.IMPORTANT_NOT_URGENT
        ) { questInfoState.priority = QuestPriority.IMPORTANT_NOT_URGENT }
        Spacer(modifier = Modifier.height(6.dp))
        PriorityOption(
            label = "Not Important, Urgent",
            color = Color(0xFFF5DEB3),
            selected = questInfoState.priority == QuestPriority.NOT_IMPORTANT_URGENT
        ) { questInfoState.priority = QuestPriority.NOT_IMPORTANT_URGENT }
        Spacer(modifier = Modifier.height(6.dp))
        PriorityOption(
            label = "Stable",
            color = Color(0xFF3B82F6),
            selected = questInfoState.priority == QuestPriority.STABLE
        ) { questInfoState.priority = QuestPriority.STABLE }
        Spacer(modifier = Modifier.height(6.dp))
        PriorityOption(
            label = "Not Important & Not Urgent",
            color = Color(0xFFD1D5DB),
            selected = questInfoState.priority == QuestPriority.NOT_IMPORTANT_NOT_URGENT
        ) { questInfoState.priority = QuestPriority.NOT_IMPORTANT_NOT_URGENT }
    }

    // Sanctions section
    Text("Sanctions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))

    // 1) Deadline selector (first item)
    var showDeadlineDialog by remember { mutableStateOf(false) }
    val deadline = questInfoState.deadlineMinutes
    Button(onClick = { showDeadlineDialog = true }, modifier = Modifier.padding(top = 6.dp)) {
        Text(
            if (deadline < 0) "Deadline: None" else "Deadline: ${formatTimeMinutes(deadline)}"
        )
    }
    if (showDeadlineDialog) {
        val initHour = if (deadline >= 0) (deadline / 60) else 18
        val initMinute = if (deadline >= 0) (deadline % 60) else 0
        val timeState = rememberTimePickerState(initialHour = initHour.coerceIn(0,23), initialMinute = initMinute.coerceIn(0,59), is24Hour = false)
        AlertDialog(
            onDismissRequest = { showDeadlineDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    questInfoState.deadlineMinutes = timeState.hour * 60 + timeState.minute
                    showDeadlineDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDeadlineDialog = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        questInfoState.deadlineMinutes = -1
                        showDeadlineDialog = false
                    }) { Text("Clear") }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Deadline Time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    TimePicker(state = timeState)
                }
            }
        )
    }

    // 2) Ban purchasing certain unlock(s) in Store for N days (UI only)
    var banUnlocks by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Switch(checked = banUnlocks, onCheckedChange = { banUnlocks = it })
        Spacer(modifier = Modifier.width(8.dp))
        Text("Ban buying selected Store unlock(s)")
    }
    if (banUnlocks) {
        // Load available unlockers once
        val context = LocalContext.current
        var unlockers by remember { mutableStateOf<List<AppUnlockerItem>>(emptyList()) }
        LaunchedEffect(Unit) {
            val dao = QuestDatabaseProvider.getInstance(context).appUnlockerItemDao()
            unlockers = dao.getAll().first()
        }

        // Days field bound to state
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = if (questInfoState.sanctionBanDays == 0) "" else questInfoState.sanctionBanDays.toString(),
                onValueChange = { v ->
                    questInfoState.sanctionBanDays = v.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
                },
                label = { Text("Days") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            var showUnlockerPicker by remember { mutableStateOf(false) }
            Button(onClick = { showUnlockerPicker = true }, modifier = Modifier.weight(2f)) {
                val count = questInfoState.sanctionSelectedUnlockerIds.size
                Text(if (count == 0) "Select unlockers" else "Selected: $count")
            }

            if (showUnlockerPicker) {
                AlertDialog(
                    onDismissRequest = { showUnlockerPicker = false },
                    confirmButton = {
                        TextButton(onClick = { showUnlockerPicker = false }) { Text("Done") }
                    },
                    dismissButton = {
                        TextButton(onClick = { questInfoState.sanctionSelectedUnlockerIds = emptySet(); showUnlockerPicker = false }) { Text("Clear") }
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Choose unlockers", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                            Box(modifier = Modifier.heightIn(max = 320.dp)) {
                                LazyColumn {
                                    items(unlockers) { item ->
                                        val checked = questInfoState.sanctionSelectedUnlockerIds.contains(item.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    questInfoState.sanctionSelectedUnlockerIds = questInfoState.sanctionSelectedUnlockerIds.toMutableSet().apply {
                                                        if (!add(item.id)) remove(item.id)
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(checked = checked, onCheckedChange = {
                                                questInfoState.sanctionSelectedUnlockerIds = questInfoState.sanctionSelectedUnlockerIds.toMutableSet().apply {
                                                    if (it) add(item.id) else remove(item.id)
                                                }
                                            })
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(item.appName)
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    // 3) Coin liquidation percentage (improved UI)
    Text("Coin liquidation", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp))
    var liquidationPercent by remember { mutableIntStateOf(questInfoState.sanctionLiquidationPercent.coerceIn(0, 100)) }
    // Preset percentage chips
    val presets = listOf(0, 10, 25, 50, 75, 100)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 6.dp)
            .then(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()))
    ) {
        presets.forEachIndexed { index, p ->
            val selected = liquidationPercent == p
            if (selected) {
                Button(onClick = { /* no-op when selected */ }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("$p%")
                }
            } else {
                OutlinedButton(onClick = {
                    liquidationPercent = p
                    questInfoState.sanctionLiquidationPercent = p
                }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("$p%")
                }
            }
            if (index != presets.lastIndex) Spacer(modifier = Modifier.width(8.dp))
        }
    }
    // Stepper controls
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = {
            val newVal = (liquidationPercent - 5).coerceAtLeast(0)
            liquidationPercent = newVal
            questInfoState.sanctionLiquidationPercent = newVal
        }) { Text("-") }
        Spacer(modifier = Modifier.width(12.dp))
        Text("${liquidationPercent}%", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedButton(onClick = {
            val newVal = (liquidationPercent + 5).coerceAtMost(100)
            liquidationPercent = newVal
            questInfoState.sanctionLiquidationPercent = newVal
        }) { Text("+") }
    }

    // 4) Phone block with API field (UI only)
    var phoneBlock by remember { mutableStateOf(questInfoState.sanctionPhoneBlock) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Switch(checked = phoneBlock, onCheckedChange = { phoneBlock = it; questInfoState.sanctionPhoneBlock = it })
        Spacer(modifier = Modifier.width(8.dp))
        Text("Phone block")
    }
    if (phoneBlock) {
        OutlinedTextField(
            value = questInfoState.sanctionPhoneApi,
            onValueChange = { questInfoState.sanctionPhoneApi = it },
            label = { Text("API endpoint") },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
    RewardSetter(questInfoState)


}

@Composable
fun RewardSetter(questInfoState: QuestInfoState) {
    var isRandom by remember { mutableStateOf(questInfoState.rewardMin != questInfoState.rewardMax) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Random Reward")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isRandom,
                onCheckedChange = { isRandom = it }
            )
        }

        if (isRandom) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = questInfoState.rewardMin.toString(),
                    onValueChange = { value ->
                        questInfoState.rewardMin = value.toIntOrNull() ?: 0
                    },
                    label = { Text("Min Reward") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = questInfoState.rewardMax.toString(),
                    onValueChange = { value ->
                        questInfoState.rewardMax = value.toIntOrNull() ?: 0
                    },
                    label = { Text("Max Reward") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OutlinedTextField(
                value = questInfoState.rewardMin.toString(),
                onValueChange = { value ->
                    val amount = value.toIntOrNull() ?: 0
                    questInfoState.rewardMin = amount
                    questInfoState.rewardMax = amount
                },
                label = { Text("Reward") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
