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

                    // AI Photo Proof is handled in SetBaseQuest under Reward

                    // Quest and Break Duration are handled in SetBaseQuest

                    val isSchedulingValid by remember(questInfoState.schedulingInfo) {
                        derivedStateOf {
                            when (questInfoState.schedulingInfo.type) {
                                SchedulingType.WEEKLY -> questInfoState.schedulingInfo.selectedDays.isNotEmpty()
                                SchedulingType.SPECIFIC_DATE -> !questInfoState.schedulingInfo.specificDate.isNullOrBlank()
                                SchedulingType.MONTHLY_DATE -> questInfoState.schedulingInfo.monthlyDate != null
                                SchedulingType.MONTHLY_BY_DAY -> questInfoState.schedulingInfo.monthlyDayOfWeek != null && questInfoState.schedulingInfo.monthlyWeekInMonth != null
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
