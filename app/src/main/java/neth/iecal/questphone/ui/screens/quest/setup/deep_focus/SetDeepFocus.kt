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
import neth.iecal.questphone.ui.screens.quest.setup.components.SetFocusTimeUI
import neth.iecal.questphone.ui.screens.quest.setup.components.SetBreakTimeUI
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.json
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val focusTimeConfig = remember { mutableStateOf(FocusTimeConfig()) }
        val breakDuration = remember { mutableStateOf(0L) }
    val minWorkSessions = remember { mutableStateOf(1) }
    val maxWorkSessions = remember { mutableStateOf(5) }
    val longBreakDuration = remember { mutableStateOf(0L) }
    val rewardPerExtraSession = remember { mutableStateOf(0) }
    val longBreakAfterSessions = remember { mutableStateOf(0) }

    val scrollState = rememberScrollState()
    val sp = QuestHelper(context)

    val isReviewDialogVisible = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if(editQuestId!=null){
            val dao = QuestDatabaseProvider.getInstance(context).questDao()
            val quest = dao.getQuest(editQuestId)
            questInfoState.fromBaseQuest(quest!!)
            val deepFocus = json.decodeFromString<DeepFocus>(quest.quest_json)
            focusTimeConfig.value = deepFocus.focusTimeConfig
            selectedApps.addAll(deepFocus.unrestrictedApps)
                        breakDuration.value = deepFocus.breakDurationInMillis
            minWorkSessions.value = deepFocus.minWorkSessions
            maxWorkSessions.value = deepFocus.maxWorkSessions
            longBreakDuration.value = deepFocus.longBreakDurationInMillis
            rewardPerExtraSession.value = deepFocus.reward_per_extra_session
            longBreakAfterSessions.value = deepFocus.long_break_after_sessions
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
        val deepFocus = DeepFocus(
            focusTimeConfig = focusTimeConfig.value,
            unrestrictedApps = selectedApps.toList(),
            breakDurationInMillis = breakDuration.value,
            minWorkSessions = minWorkSessions.value,
            maxWorkSessions = maxWorkSessions.value,
            longBreakDurationInMillis = longBreakDuration.value,
            reward_per_extra_session = rewardPerExtraSession.value,
            long_break_after_sessions = longBreakAfterSessions.value,
            nextFocusDurationInMillis = focusTimeConfig.value.initialTimeInMs
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
                    SetBaseQuest(questInfoState)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("AI Photo Proof", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = questInfoState.aiPhotoProof,
                            onCheckedChange = { questInfoState.aiPhotoProof = it }
                        )
                    }

                    AnimatedVisibility(visible = questInfoState.aiPhotoProof) {
                        OutlinedTextField(
                            value = questInfoState.aiPhotoProofDescription,
                            onValueChange = { questInfoState.aiPhotoProofDescription = it },
                            label = { Text("AI Photo Proof Description") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    // QR proof toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("QR Proof", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = questInfoState.qrProof,
                            onCheckedChange = { questInfoState.qrProof = it }
                        )
                    }

                    OutlinedButton(
                        onClick = { showDialog.value = true },
                    ) {

                        Text(
                            text = "Selected App Exceptions ${selectedApps.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    SetFocusTimeUI(focusTimeConfig)

                    SetBreakTimeUI(breakDuration)

                    OutlinedTextField(
                        value = minWorkSessions.value.toString(),
                        onValueChange = { minWorkSessions.value = it.toIntOrNull() ?: 1 },
                        label = { Text("Min Work Sessions") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = maxWorkSessions.value.toString(),
                        onValueChange = { maxWorkSessions.value = it.toIntOrNull() ?: 5 },
                        label = { Text("Max Work Sessions") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = (longBreakDuration.value / 60000).toString(),
                        onValueChange = { longBreakDuration.value = (it.toLongOrNull() ?: 0L) * 60000 },
                        label = { Text("Long Break Duration (minutes)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rewardPerExtraSession.value.toString(),
                        onValueChange = { rewardPerExtraSession.value = it.toIntOrNull() ?: 0 },
                        label = { Text("Reward per Extra Session") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = longBreakAfterSessions.value.toString(),
                        onValueChange = { longBreakAfterSessions.value = it.toIntOrNull() ?: 0 },
                        label = { Text("Long Break After Sessions") },
                        modifier = Modifier.fillMaxWidth()
                    )

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