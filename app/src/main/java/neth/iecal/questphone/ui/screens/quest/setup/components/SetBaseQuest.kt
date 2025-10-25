package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.data.IntegrationId
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.Dp
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

@Composable
private fun PriorityTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(if (selected) Modifier.border(2.dp, tint, MaterialTheme.shapes.medium) else Modifier)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBaseQuest(
    questInfoState: QuestInfoState,
    isTimeRangeSupported: Boolean = true,
    showDurationBreakSection: Boolean = true,
    rewardExtraContent: (@Composable (Boolean) -> Unit)? = null,
    afterTimeContent: (@Composable () -> Unit)? = null,
    diamondContent: (@Composable () -> Unit)? = null
) {

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

    // Removed anti-abuse restriction message and check to allow performing quests today.
    EnhancedScheduling(questInfoState)


    OutlinedTextField(
        value = questInfoState.instructions,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = { questInfoState.instructions = it }, // Direct update
        label = { Text("Instructions") },
        modifier = Modifier.fillMaxWidth()
            .height(200.dp)
    )
    // Below this line we re-structure UI per spec
    if(isTimeRangeSupported){
        // Header outside card
        Text("Time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Card(
            modifier = Modifier.padding(top = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                SetTimeRange(questInfoState)
            }
        }
    }

    // Deadline section removed; Deadline selector is now part of the Time section UI.

    if (showDurationBreakSection) {
        // Duration and break numeric inputs
        Text("Duration and break", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Card(
            modifier = Modifier.padding(top = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (questInfoState.questDurationMinutes <= 0) "" else questInfoState.questDurationMinutes.toString(),
                        onValueChange = { v ->
                            questInfoState.questDurationMinutes = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                        },
                        label = { Text("Quest (min)", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = if (questInfoState.breakDurationMinutes <= 0) "" else questInfoState.breakDurationMinutes.toString(),
                        onValueChange = { v ->
                            questInfoState.breakDurationMinutes = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                        },
                        label = { Text("Break (min)", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Custom content requested to appear right after Time section
    afterTimeContent?.invoke()

    // Priority selector replicated from quest_creator.html: 2x2 grid + full-width bottom option
    Text("Priority", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityTile(
                label = "Important & Urgent",
                icon = Icons.Outlined.ErrorOutline,
                tint = Color(0xFFEF4444),
                selected = questInfoState.priority == QuestPriority.IMPORTANT_URGENT,
                onClick = { questInfoState.priority = QuestPriority.IMPORTANT_URGENT },
                modifier = Modifier.weight(1f)
            )
            PriorityTile(
                label = "Important, not urgent",
                icon = Icons.Outlined.CheckBoxOutlineBlank,
                tint = Color(0xFF10B981),
                selected = questInfoState.priority == QuestPriority.IMPORTANT_NOT_URGENT,
                onClick = { questInfoState.priority = QuestPriority.IMPORTANT_NOT_URGENT },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityTile(
                label = "Not important, urgent",
                icon = Icons.Outlined.Warning,
                tint = Color(0xFFF59E0B),
                selected = questInfoState.priority == QuestPriority.NOT_IMPORTANT_URGENT,
                onClick = { questInfoState.priority = QuestPriority.NOT_IMPORTANT_URGENT },
                modifier = Modifier.weight(1f)
            )
            PriorityTile(
                label = "Stable task",
                icon = Icons.Outlined.HourglassEmpty,
                tint = Color(0xFF9CA3AF),
                selected = questInfoState.priority == QuestPriority.STABLE,
                onClick = { questInfoState.priority = QuestPriority.STABLE },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        PriorityTile(
            label = "Not important, not urgent",
            icon = Icons.Outlined.SwapHoriz,
            tint = Color(0xFF9CA3AF),
            selected = questInfoState.priority == QuestPriority.NOT_IMPORTANT_NOT_URGENT,
            onClick = { questInfoState.priority = QuestPriority.NOT_IMPORTANT_NOT_URGENT },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Reward and AI Photo Proof grouped in a Card
    Text("Reward", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    Card(
        modifier = Modifier.padding(top = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Reward section should be before sanctions
            RewardSetter(questInfoState, extraContent = rewardExtraContent, diamondContent = diamondContent)

            // AI Photo Proof (under Reward)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text("AI Photo Proof", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = questInfoState.aiPhotoProof,
                    onCheckedChange = { questInfoState.aiPhotoProof = it }
                )
            }
            // QR Proof (DeepFocus only) under AI Photo Proof
            if (questInfoState.integrationId == IntegrationId.DEEP_FOCUS) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text("QR Proof", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = questInfoState.qrProof,
                        onCheckedChange = { questInfoState.qrProof = it }
                    )
                }
            }
            AnimatedVisibility(visible = questInfoState.aiPhotoProof) {
                OutlinedTextField(
                    value = questInfoState.aiPhotoProofDescription,
                    onValueChange = { questInfoState.aiPhotoProofDescription = it },
                    label = { Text("AI Photo Proof Description", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
        }
    }

    // Sanctions section (three toggles with inline expanding options)
    Text("Sanctions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    Card(
        modifier = Modifier.padding(top = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            val context = LocalContext.current
            val appUnlockerDao = remember { QuestDatabaseProvider.getInstance(context).appUnlockerItemDao() }
            var allUnlockers by remember { mutableStateOf<List<AppUnlockerItem>>(emptyList()) }
            LaunchedEffect(Unit) {
                allUnlockers = appUnlockerDao.getAll().first()
            }

            // 1) Ban on buying an unlocker
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            var banUnlockersEnabled by remember { mutableStateOf(questInfoState.sanctionBanDays > 0) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Ban on buying an unlocker", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = banUnlockersEnabled,
                    onCheckedChange = {
                        banUnlockersEnabled = it
                        if (it) {
                            if (questInfoState.sanctionBanDays <= 0) questInfoState.sanctionBanDays = 1
                        } else {
                            questInfoState.sanctionBanDays = 0
                            questInfoState.sanctionSelectedUnlockerIds = emptySet()
                        }
                    }
                )
            }
            AnimatedVisibility(visible = banUnlockersEnabled) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = (questInfoState.sanctionBanDays.takeIf { it > 0 } ?: 1).toString(),
                        onValueChange = { v ->
                            questInfoState.sanctionBanDays = v.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceIn(1,365) ?: 1
                        },
                        label = { Text("Ban days", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select unlockers to ban", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column {
                        allUnlockers.forEach { item ->
                            val checked = questInfoState.sanctionSelectedUnlockerIds.contains(item.id)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    questInfoState.sanctionSelectedUnlockerIds = if (checked)
                                        questInfoState.sanctionSelectedUnlockerIds - item.id else
                                        questInfoState.sanctionSelectedUnlockerIds + item.id
                                }
                                .padding(vertical = 6.dp)) {
                                Checkbox(checked = checked, onCheckedChange = {
                                    questInfoState.sanctionSelectedUnlockerIds = if (checked)
                                        questInfoState.sanctionSelectedUnlockerIds - item.id else
                                        questInfoState.sanctionSelectedUnlockerIds + item.id
                                })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.appName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (allUnlockers.isEmpty()) {
                            Text("No unlockers in Store", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 2) Percentage of coin liquidation
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            var liquidationEnabled by remember { mutableStateOf(questInfoState.sanctionLiquidationPercent > 0) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Percentage of coin liquidation", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = liquidationEnabled,
                    onCheckedChange = { enabled ->
                        liquidationEnabled = enabled
                        questInfoState.sanctionLiquidationPercent = if (enabled) 50 else 0
                    }
                )
            }
            AnimatedVisibility(visible = liquidationEnabled) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    PercentDropdown(
                        label = "Liquidation percent",
                        percent = questInfoState.sanctionLiquidationPercent,
                        onSelect = { questInfoState.sanctionLiquidationPercent = it }
                    )
                }
            }

            // 3) Phone block
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Phone block", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = questInfoState.sanctionPhoneBlock,
                    onCheckedChange = { enabled ->
                        questInfoState.sanctionPhoneBlock = enabled
                    }
                )
            }
            AnimatedVisibility(visible = questInfoState.sanctionPhoneBlock) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = questInfoState.sanctionPhoneApi,
                        onValueChange = { questInfoState.sanctionPhoneApi = it },
                        label = { Text("Phone block API", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("https://...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    // Finally other settings
    AutoDestruct(questInfoState)


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinutesDropdown(
    label: String,
    minutes: Int,
    options: List<Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = if (minutes <= 0) "--" else "$minutes min"
    var anchorWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    
    // Using a Box with clickable as a fallback
    Box(
        modifier = modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
            .onGloballyPositioned { c ->
                with(density) { anchorWidth = c.size.width.toDp() }
            }
            .clickable { expanded = true }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label: $selectedLabel",
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }
    }
    
    // Popup dropdown (Material3) with scroll & anchored width
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .width(anchorWidth)
            .heightIn(max = 220.dp)
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState())
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text("$opt min") },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PercentDropdown(
    label: String,
    percent: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val clamped = percent.coerceIn(0, 100)
    val selectedLabel = "$clamped %"
    var percentAnchorWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
            .clickable { expanded = !expanded }
            .onGloballyPositioned { c ->
                with(density) { percentAnchorWidth = c.size.width.toDp() }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Text(selectedLabel, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }
    }

    val options = remember { (0..100 step 5).toList() }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .width(percentAnchorWidth)
            .heightIn(max = 220.dp)
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState())
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text("$opt %") },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RewardSetter(
    questInfoState: QuestInfoState,
    extraContent: (@Composable (Boolean) -> Unit)? = null,
    diamondContent: (@Composable () -> Unit)? = null
) {
    var isRandom by remember { mutableStateOf(questInfoState.rewardMin != questInfoState.rewardMax) }
    var isDiamondEnabled by remember { mutableStateOf(questInfoState.diamondReward > 0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Random Reward", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isRandom,
                onCheckedChange = { isRandom = it }
            )
        }

        if (isRandom) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (questInfoState.rewardMin <= 0) "" else questInfoState.rewardMin.toString(),
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }.take(5)
                        questInfoState.rewardMin = digits.toIntOrNull() ?: 0
                    },
                    label = { Text("Min Reward", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = if (questInfoState.rewardMax <= 0) "" else questInfoState.rewardMax.toString(),
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }.take(5)
                        questInfoState.rewardMax = digits.toIntOrNull() ?: 0
                    },
                    label = { Text("Max Reward", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            // Extra content below main reward inputs
            extraContent?.invoke(true)
        } else {
            OutlinedTextField(
                value = if (questInfoState.rewardMin == questInfoState.rewardMax && questInfoState.rewardMin > 0) questInfoState.rewardMin.toString() else "",
                onValueChange = { value ->
                    val digits = value.filter { it.isDigit() }.take(5)
                    val amount = digits.toIntOrNull() ?: 0
                    questInfoState.rewardMin = amount
                    questInfoState.rewardMax = amount
                },
                label = { Text("Reward", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            // Extra content below main reward input
            extraContent?.invoke(false)
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (diamondContent != null) {
            diamondContent()
        } else {
            // Default diamond section (single field)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Diamond Reward", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isDiamondEnabled,
                    onCheckedChange = {
                        isDiamondEnabled = it
                        if (!it) questInfoState.diamondReward = 0
                    }
                )
            }
            if (isDiamondEnabled) {
                OutlinedTextField(
                    value = if (questInfoState.diamondReward <= 0) "" else questInfoState.diamondReward.toString(),
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }.take(4)
                        questInfoState.diamondReward = digits.toIntOrNull() ?: 0
                    },
                    label = { Text("Diamond reward", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                PercentDropdown(
                    label = "Drop Chance",
                    percent = questInfoState.diamondDropChance,
                    onSelect = { questInfoState.diamondDropChance = it }
                )
            }
        }
    }
}
