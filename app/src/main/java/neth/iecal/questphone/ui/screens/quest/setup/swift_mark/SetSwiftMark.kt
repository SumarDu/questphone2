package neth.iecal.questphone.ui.screens.quest.setup.swift_mark

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.SchedulingType
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.utils.SchedulingUtils
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.ui.screens.quest.setup.ReviewDialog
import neth.iecal.questphone.ui.screens.quest.setup.components.SetBaseQuest
import neth.iecal.questphone.utils.QuestHelper


@SuppressLint("UnrememberedMutableState")
@Composable
fun SetSwiftMark(editQuestId:String? = null,navController: NavHostController) {

    val context = LocalContext.current
    val settingsRepository = SettingsRepository(context)
    val settings by settingsRepository.settings.collectAsState()

    val questInfoState =
        remember {
            QuestInfoState(
                initialIntegrationId = IntegrationId.SWIFT_MARK,
                initialRewardMin = 1,
                initialRewardMax = 1
            )
        }

    val sp = QuestHelper(context)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val isReviewDialogVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit){
        if(editQuestId!=null){
            val dao = QuestDatabaseProvider.getInstance(context).questDao()
            val quest = dao.getQuestById(editQuestId)
            if (quest != null) {
                questInfoState.fromBaseQuest(quest)
            } else {
                // Handle case where quest is not found
                navController.popBackStack()
            }
        }
    }

    if (isReviewDialogVisible.value) {
        val baseQuest =
            questInfoState.toBaseQuest(null)
        ReviewDialog(
            items = listOf(
                baseQuest
            ),

            onConfirm = {
                scope.launch {
                    val dao = QuestDatabaseProvider.getInstance(context).questDao()
                    // Set proper expiration date based on scheduling type
                    val questWithExpiration = baseQuest.copy(
                        auto_destruct = SchedulingUtils.getExpirationDate(baseQuest.scheduling_info, baseQuest.auto_destruct)
                    )
                    dao.upsertQuest(questWithExpiration)
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
                        text = "Swift Quest",
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

                    // Quest and Break Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = questInfoState.questDurationMinutes.toString(),
                            onValueChange = { questInfoState.questDurationMinutes = it.toIntOrNull() ?: 0 },
                            label = { Text("Quest (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = questInfoState.breakDurationMinutes.toString(),
                            onValueChange = { questInfoState.breakDurationMinutes = it.toIntOrNull() ?: 0 },
                            label = { Text("Break (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val isSchedulingValid by remember(questInfoState.schedulingInfo) {
                        derivedStateOf {
                            when (questInfoState.schedulingInfo.type) {
                                SchedulingType.WEEKLY -> questInfoState.schedulingInfo.selectedDays.isNotEmpty()
                                SchedulingType.SPECIFIC_DATE -> !questInfoState.schedulingInfo.specificDate.isNullOrBlank()
                                SchedulingType.MONTHLY_DATE -> questInfoState.schedulingInfo.monthlyDate != null
                                SchedulingType.MONTHLY_LAST_DAY -> questInfoState.schedulingInfo.monthlyLastDayOfWeek != null
                            }
                        }
                    }

                    Button(
                        enabled = isSchedulingValid && (settings.isQuestCreationEnabled || editQuestId != null),
                        onClick = {
                            isReviewDialogVisible.value = true
                        },
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
