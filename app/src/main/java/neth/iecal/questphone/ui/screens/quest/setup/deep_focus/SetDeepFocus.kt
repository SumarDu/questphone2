package neth.iecal.questphone.ui.screens.quest.setup.deep_focus

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.AppInfo
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.data.quest.focus.DeepFocus
import neth.iecal.questphone.data.quest.focus.FocusTimeConfig
import neth.iecal.questphone.ui.screens.quest.setup.ReviewDialog
import neth.iecal.questphone.ui.screens.quest.setup.components.SetBaseQuest
import neth.iecal.questphone.ui.screens.quest.setup.components.convertToMinutes
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.json
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.utils.reloadApps

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetDeepFocus(editQuestId:String? = null,navController: NavHostController) {
    val context = LocalContext.current
    val settingsRepository = SettingsRepository(context)
    val settings by settingsRepository.settings.collectAsState()
    val apps = remember { mutableStateOf(emptyList<AppInfo>()) }

    val showDialog = remember { mutableStateOf(false) }
    var selectedApps = remember { mutableStateListOf<String>() }
    val questInfoState = remember { QuestInfoState(initialIntegrationId = IntegrationId.DEEP_FOCUS) }
    // Initialize with zeros so UI shows placeholders (e.g., Session (min) placeholder "1") until user types
    val focusTimeConfig = remember {
        mutableStateOf(
            FocusTimeConfig(
                initialTime = "0",
                finalTime = "0",
                incrementTime = "0",
                initialUnit = "m",
                finalUnit = "m",
                incrementUnit = "m"
            )
        )
    }
        val breakDuration = remember { mutableStateOf(0L) }
    // Start at 0 so placeholder defaults are shown and disappear on typing
    val minWorkSessions = remember { mutableStateOf(0) }
    val maxWorkSessions = remember { mutableStateOf(0) }
    val longBreakDuration = remember { mutableStateOf(0L) }
    // Start at 0 so placeholder default is shown (10)
    val rewardPerExtraSession = remember { mutableStateOf(0) }
    // Independent extra reward randomness state
    var isExtraRandom by remember { mutableStateOf(false) }
    // Extra reward random range values
    var extraRewardMin by remember { mutableStateOf(0) }
    var extraRewardMax by remember { mutableStateOf(0) }
    val longBreakAfterSessions = remember { mutableStateOf(0) }
    // Diamond rewards UI state (shared across UI and ReviewDialog save block)
    var isDiamondEnabled by remember { mutableStateOf(false) }
    var diamondRegular by remember { mutableStateOf(0) }
    var diamondExtra by remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()
    val sp = QuestHelper(context)

    val isReviewDialogVisible = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if(editQuestId!=null){
            val dao = QuestDatabaseProvider.getInstance(context).questDao()
            val quest = dao.getQuestById(editQuestId)
            if (quest != null) {
                questInfoState.fromBaseQuest(quest)
                val deepFocus = kotlin.runCatching { json.decodeFromString<DeepFocus>(quest.quest_json) }.getOrElse { DeepFocus() }
                focusTimeConfig.value = deepFocus.focusTimeConfig
                selectedApps.addAll(deepFocus.unrestrictedApps)
                breakDuration.value = deepFocus.breakDurationInMillis
                minWorkSessions.value = deepFocus.minWorkSessions
                maxWorkSessions.value = deepFocus.maxWorkSessions
                longBreakDuration.value = deepFocus.longBreakDurationInMillis
                // Load extra reward settings
                rewardPerExtraSession.value = deepFocus.reward_per_extra_session
                // If min/max present, prefer random mode and populate fields
                if (deepFocus.reward_extra_min > 0 && deepFocus.reward_extra_max >= deepFocus.reward_extra_min) {
                    isExtraRandom = true
                    extraRewardMin = deepFocus.reward_extra_min
                    extraRewardMax = deepFocus.reward_extra_max
                } else {
                    isExtraRandom = false
                    // Fall back to fixed per-session reward
                    extraRewardMin = 0
                    extraRewardMax = 0
                }
                longBreakAfterSessions.value = deepFocus.long_break_after_sessions
                // Load diamond rewards
                diamondRegular = deepFocus.diamond_reward_regular
                diamondExtra = deepFocus.diamond_reward_extra
                isDiamondEnabled = (deepFocus.diamond_reward_regular > 0 || deepFocus.diamond_reward_extra > 0)
            }
        }
    }
    LaunchedEffect(apps) {
        apps.value = reloadApps(context.packageManager, context).getOrNull() ?: emptyList()
    }

    if (showDialog.value) {
        SelectAppsDialog(
            apps = apps,
            selectedApps = selectedApps,
            onDismiss = {
                showDialog.value = false
            }
        )
    }
    if (isReviewDialogVisible.value) {
        // Normalize defaults for fields left empty (0) before saving
        // Focus times (minutes)
        val effInitialMin = convertToMinutes(focusTimeConfig.value.initialTime, focusTimeConfig.value.initialUnit).let { if (it <= 0) 1 else it }
        val effIncrementMin = convertToMinutes(focusTimeConfig.value.incrementTime, focusTimeConfig.value.incrementUnit).let { if (it <= 0) 1 else it }
        val effFinalMin = convertToMinutes(focusTimeConfig.value.finalTime, focusTimeConfig.value.finalUnit).let { if (it <= 0) 10 else it }

        val normalizedConfig = focusTimeConfig.value.copy(
            initialTime = effInitialMin.toString(), initialUnit = "m",
            incrementTime = effIncrementMin.toString(), incrementUnit = "m",
            finalTime = effFinalMin.toString(), finalUnit = "m",
        )

        // Work sessions and breaks
        val effMinSessions = if (minWorkSessions.value <= 0) 2 else minWorkSessions.value
        val effMaxSessions = if (maxWorkSessions.value <= 0) 4 else maxWorkSessions.value
        val effBreakMin = ((breakDuration.value / 60000L).toInt()).let { if (it <= 0) 1 else it }
        val effLongBreakMin = ((longBreakDuration.value / 60000L).toInt()).let { if (it <= 0) 10 else it }
        val effLongAfter = if (longBreakAfterSessions.value <= 0) 2 else longBreakAfterSessions.value

        // Rewards
        if (questInfoState.rewardMin <= 0) questInfoState.rewardMin = 5
        if (questInfoState.rewardMax <= 0) questInfoState.rewardMax = questInfoState.rewardMin
        val effExtraReward = if (rewardPerExtraSession.value <= 0) 10 else rewardPerExtraSession.value

        val deepFocus = DeepFocus(
            focusTimeConfig = normalizedConfig,
            unrestrictedApps = selectedApps.toList(),
            breakDurationInMillis = effBreakMin * 60000L,
            minWorkSessions = effMinSessions,
            maxWorkSessions = effMaxSessions,
            longBreakDurationInMillis = effLongBreakMin * 60000L,
            reward_per_extra_session = if (isExtraRandom) 0 else effExtraReward,
            reward_extra_min = if (isExtraRandom) extraRewardMin.coerceAtLeast(0) else 0,
            reward_extra_max = if (isExtraRandom) extraRewardMax.coerceAtLeast(extraRewardMin.coerceAtLeast(0)) else 0,
            // Persist diamond rewards
            diamond_reward_regular = if (isDiamondEnabled) diamondRegular else 0,
            diamond_reward_extra = if (isDiamondEnabled) diamondExtra else 0,
            long_break_after_sessions = effLongAfter,
            nextFocusDurationInMillis = effInitialMin * 60000L
        )
        val baseQuest =
            questInfoState.toBaseQuest<DeepFocus>(deepFocus)

        ReviewDialog(
            items = listOf(
                baseQuest, deepFocus
            ),

            onConfirm = {
                scope.launch {
                    val dao = QuestDatabaseProvider.getInstance(context).questDao()
                    dao.upsertQuest(baseQuest)
                }
                isReviewDialogVisible.value = false
                navController.popBackStack()
            },
            onDismiss = {
                isReviewDialogVisible.value = false
            }
        )
    }
    Scaffold()
    { paddingValues ->

        Box(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)

            ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(top = 32.dp)
                ) {

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = "Deep Focus ",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    // Base quest UI with DeepFocus specifics:
                    // - Hide default Duration/Break section (we'll render custom one below)
                    // - Provide trailing content for Reward (Extra reward in the same row)
                    // Local UI-only state moved to top-level remembers

                    SetBaseQuest(
                        questInfoState = questInfoState,
                        isTimeRangeSupported = true,
                        showDurationBreakSection = false,
                        rewardExtraContent = { _ ->
                            // Independent toggle for extra reward randomness
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                                Text("Random extra reward", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(8.dp))
                                Switch(checked = isExtraRandom, onCheckedChange = { isExtraRandom = it })
                            }
                            if (isExtraRandom) {
                                // Min/Max Extra Reward in one row
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                    OutlinedTextField(
                                        value = if (extraRewardMin <= 0) "" else extraRewardMin.toString(),
                                        onValueChange = { v -> 
                                            extraRewardMin = v.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0
                                        },
                                        label = { Text("Min extra reward", style = MaterialTheme.typography.bodySmall) },
                                        placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = if (extraRewardMax <= 0) "" else extraRewardMax.toString(),
                                        onValueChange = { v -> 
                                            extraRewardMax = v.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0
                                        },
                                        label = { Text("Max extra reward", style = MaterialTheme.typography.bodySmall) },
                                        placeholder = { Text("10", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                // Single Extra reward input occupying its own row
                                OutlinedTextField(
                                    value = if (rewardPerExtraSession.value <= 0) "" else rewardPerExtraSession.value.toString(),
                                    onValueChange = { rewardPerExtraSession.value = it.toIntOrNull() ?: 0 },
                                    label = { Text("Extra reward", style = MaterialTheme.typography.bodySmall) },
                                    placeholder = { Text("10", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                )
                            }
                        },
                        diamondContent = {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Diamond Reward", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isDiamondEnabled,
                                    onCheckedChange = {
                                        isDiamondEnabled = it
                                        if (!it) {
                                            diamondRegular = 0
                                            diamondExtra = 0
                                        }
                                    }
                                )
                            }
                            if (isDiamondEnabled) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = if (diamondRegular <= 0) "" else diamondRegular.toString(),
                                        onValueChange = { v ->
                                            val d = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                                            diamondRegular = d
                                        },
                                        label = { Text("Diamonds (regular)", style = MaterialTheme.typography.bodySmall) },
                                        placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = if (diamondExtra <= 0) "" else diamondExtra.toString(),
                                        onValueChange = { v ->
                                            val d = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                                            diamondExtra = d
                                        },
                                        label = { Text("Diamonds (extra)", style = MaterialTheme.typography.bodySmall) },
                                        placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = questInfoState.diamondDropChance.coerceIn(0, 100).toString(),
                                    onValueChange = { v ->
                                        val pct = v.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceIn(0, 100) ?: 0
                                        questInfoState.diamondDropChance = pct
                                    },
                                    label = { Text("Drop Chance (%)", style = MaterialTheme.typography.bodySmall) },
                                    placeholder = { Text("100", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        afterTimeContent = {
                            // Custom Duration/Break section adapted for DeepFocus
                            Text("Duration and break", style = MaterialTheme.typography.titleMedium)
                            androidx.compose.material3.Card(
                                modifier = Modifier.padding(top = 6.dp),
                                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    // Session (min) — maps to initial focus time, force minutes unit
                                    val initialMinutes = convertToMinutes(focusTimeConfig.value.initialTime, focusTimeConfig.value.initialUnit)
                                    OutlinedTextField(
                                        value = if (initialMinutes <= 0) "" else initialMinutes.toString(),
                                        onValueChange = { v ->
                                            val m = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                                            focusTimeConfig.value = focusTimeConfig.value.copy(initialTime = m.toString(), initialUnit = "m")
                                        },
                                        label = { Text("Session (min)", style = MaterialTheme.typography.bodySmall) },
                                        placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    var showAdvanced by remember { mutableStateOf(false) }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                                        Text("Advanced focus growth", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.width(8.dp))
                                        Switch(checked = showAdvanced, onCheckedChange = { showAdvanced = it })
                                    }

                                    AnimatedVisibility(visible = showAdvanced) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                                val incr = convertToMinutes(focusTimeConfig.value.incrementTime, focusTimeConfig.value.incrementUnit)
                                                OutlinedTextField(
                                                    value = if (incr <= 0) "" else incr.toString(),
                                                    onValueChange = { v ->
                                                        val m = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                                                        focusTimeConfig.value = focusTimeConfig.value.copy(incrementTime = m.toString(), incrementUnit = "m")
                                                    },
                                                    label = { Text("increment Daily by", style = MaterialTheme.typography.bodySmall) },
                                                    placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                val goal = convertToMinutes(focusTimeConfig.value.finalTime, focusTimeConfig.value.finalUnit)
                                                OutlinedTextField(
                                                    value = if (goal <= 0) "" else goal.toString(),
                                                    onValueChange = { v ->
                                                        val m = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                                                        focusTimeConfig.value = focusTimeConfig.value.copy(finalTime = m.toString(), finalUnit = "m")
                                                    },
                                                    label = { Text("Goal Focus Time", style = MaterialTheme.typography.bodySmall) },
                                                    placeholder = { Text("10", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    // Min/Max Work Sessions in one row
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                        OutlinedTextField(
                                            value = if (minWorkSessions.value <= 0) "" else minWorkSessions.value.toString(),
                                            onValueChange = { minWorkSessions.value = it.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0 },
                                            label = { Text("Min Work Sessions", style = MaterialTheme.typography.bodySmall) },
                                            placeholder = { Text("2", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        OutlinedTextField(
                                            value = if (maxWorkSessions.value <= 0) "" else maxWorkSessions.value.toString(),
                                            onValueChange = { maxWorkSessions.value = it.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0 },
                                            label = { Text("Max Work Sessions", style = MaterialTheme.typography.bodySmall) },
                                            placeholder = { Text("4", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Break, Long Break, Long after in one row
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                        val breakMin = (breakDuration.value / 60000L).toInt()
                                        OutlinedTextField(
                                            value = if (breakMin <= 0) "" else breakMin.toString(),
                                            onValueChange = { v ->
                                                val m = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                                                breakDuration.value = (m * 60000L)
                                            },
                                            label = { Text("Break", style = MaterialTheme.typography.bodySmall) },
                                            placeholder = { Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        val longBreakMin = (longBreakDuration.value / 60000L).toInt()
                                        OutlinedTextField(
                                            value = if (longBreakMin <= 0) "" else longBreakMin.toString(),
                                            onValueChange = { v ->
                                                val m = v.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                                                longBreakDuration.value = (m * 60000L)
                                            },
                                            label = { Text("Long Break", style = MaterialTheme.typography.bodySmall) },
                                            placeholder = { Text("10", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        OutlinedTextField(
                                            value = if (longBreakAfterSessions.value <= 0) "" else longBreakAfterSessions.value.toString(),
                                            onValueChange = { longBreakAfterSessions.value = it.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0 },
                                            label = { Text("Long after", style = MaterialTheme.typography.bodySmall) },
                                            placeholder = { Text("2", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    )

                    // AI Photo Proof and QR Proof are handled in SetBaseQuest under Reward

                    OutlinedButton(
                        onClick = { showDialog.value = true },
                    ) {

                        Text(
                            text = "Selected App Exceptions ${selectedApps.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = {
                            isReviewDialogVisible.value = true

                        },
                        enabled =
                            questInfoState.selectedDays.isNotEmpty() &&
                            (settings.isQuestCreationEnabled || editQuestId != null),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Done"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if(editQuestId==null) "Create Quest" else "Save Changes",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Spacer(Modifier.size(100.dp))
                }

            }
        }


    }

}