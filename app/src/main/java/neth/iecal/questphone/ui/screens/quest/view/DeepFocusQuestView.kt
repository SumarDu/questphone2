package neth.iecal.questphone.ui.screens.quest.view

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import android.util.Log
import neth.iecal.questphone.ui.screens.launcher.TimerViewModel
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.addCoins
import neth.iecal.questphone.data.game.getUserInfo
import neth.iecal.questphone.data.game.xpToRewardForQuest
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.focus.DeepFocusSessionLog
import neth.iecal.questphone.data.remote.SupabaseSyncService
import neth.iecal.questphone.data.quest.focus.DeepFocus
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsInfo
import neth.iecal.questphone.services.AppBlockerService
import neth.iecal.questphone.services.INTENT_ACTION_START_DEEP_FOCUS
import neth.iecal.questphone.services.INTENT_ACTION_STOP_DEEP_FOCUS
import neth.iecal.questphone.services.ServiceInfo
import neth.iecal.questphone.ui.screens.quest.checkForRewards
import neth.iecal.questphone.ui.screens.quest.calculateQuestReward
import neth.iecal.questphone.ui.screens.quest.view.components.MdPad
import neth.iecal.questphone.ui.theme.JetBrainsMonoFont
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.formatHour
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.json
import neth.iecal.questphone.utils.sendRefreshRequest
import java.util.UUID


private const val NOTIFICATION_CHANNEL_ID = "focus_timer_channel"
private const val NOTIFICATION_ID = 1001

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeepFocusQuestView(
    commonQuestInfo: CommonQuestInfo
) {
    val context = LocalContext.current
        var deepFocus by remember(commonQuestInfo) { mutableStateOf(json.decodeFromString<DeepFocus>(commonQuestInfo.quest_json)) }

    LaunchedEffect(deepFocus) {
        val updatedJson = json.encodeToString(deepFocus)
        if (updatedJson != commonQuestInfo.quest_json) {
            commonQuestInfo.quest_json = updatedJson
            val dao = QuestDatabaseProvider.getInstance(context).questDao()
            dao.upsertQuest(commonQuestInfo)
        }
    }
    val duration = deepFocus.nextFocusDurationInMillis
    val isInTimeRange = remember { mutableStateOf(QuestHelper.Companion.isInTimeRange(commonQuestInfo)) }

    val timerViewModel: TimerViewModel = viewModel()
    val timerMode by timerViewModel.timerMode.collectAsState()
    val timerText by timerViewModel.timerText.collectAsState()
    val timerState by timerViewModel.timerState.collectAsState()

    val isQuestRunning by remember(timerMode, timerState) {
        mutableStateOf(timerMode == TimerMode.QUEST_COUNTDOWN && timerState.activeQuestId == commonQuestInfo.id)
    }

    val isQuestComplete = remember {
        mutableStateOf(
            // The quest is complete for the day only if the last completion was today
            // AND the session counter has been reset to 0.
            commonQuestInfo.last_completed_on == getCurrentDate() && deepFocus.completedWorkSessions == 0
        )
    }
    val showSessionReviewDialog = remember { mutableStateOf(false) }
    val sessionNumberForReview = remember { mutableIntStateOf(0) }
    val showQuestCompletionReviewDialog = remember { mutableStateOf(false) }
    val showStudyTopicDialog = remember { mutableStateOf(false) }
    val currentStudyTopic = remember { mutableStateOf("") }

    val isFailed = remember { mutableStateOf(QuestHelper(context).isOver(commonQuestInfo)) }

    val dao = QuestDatabaseProvider.getInstance(context).questDao()
    val deepFocusSessionLogDao = QuestDatabaseProvider.getInstance(context).deepFocusSessionLogDao()
    val scope = rememberCoroutineScope()

    var sessionStartTime by remember { mutableStateOf(0L) }

    val progress by remember(isQuestComplete.value, isFailed.value, timerMode, timerText) {
        derivedStateOf {
            if (isQuestComplete.value || isFailed.value) {
                1f
            } else if (timerMode == TimerMode.QUEST_COUNTDOWN && commonQuestInfo.quest_duration_minutes > 0) {
                val durationInMillis = TimeUnit.MINUTES.toMillis(commonQuestInfo.quest_duration_minutes.toLong())
                val remainingMillis = timerText.split(':').let { parts ->
                    if (parts.size == 2) {
                        val minutes = parts[0].toLongOrNull() ?: 0
                        val seconds = parts[1].toLongOrNull() ?: 0
                        TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds)
                    } else {
                        durationInMillis
                    }
                }
                (1f - (remainingMillis.toFloat() / durationInMillis.toFloat())).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val userInfo = getUserInfo(LocalContext.current)

    

    fun onQuestComplete() {
    // Increment session count, update next focus duration, and capture the new state
    val newCompletedWorkSessions = deepFocus.completedWorkSessions + 1
    deepFocus = deepFocus.copy(
        completedWorkSessions = newCompletedWorkSessions,
        nextFocusDurationInMillis = if (deepFocus.nextFocusDurationInMillis < deepFocus.focusTimeConfig.finalTimeInMs) {
            minOf(
                deepFocus.nextFocusDurationInMillis + deepFocus.focusTimeConfig.incrementTimeInMs,
                deepFocus.focusTimeConfig.finalTimeInMs
            )
        } else {
            deepFocus.nextFocusDurationInMillis
        }
    )
    sessionNumberForReview.value = newCompletedWorkSessions

    showSessionReviewDialog.value = true


    val sessionsAfterComplete = deepFocus.completedWorkSessions

    // Check if the minimum number of sessions has just been completed to grant rewards.
    if (sessionsAfterComplete == deepFocus.minWorkSessions) {
        val rewardAmount = calculateQuestReward(commonQuestInfo)
        checkForRewards(commonQuestInfo, rewardAmount)

        // Also log this to stats
        scope.launch {
            val statsDao = StatsDatabaseProvider.getInstance(context).statsDao()
            statsDao.upsertStats(
                StatsInfo(
                    id = UUID.randomUUID().toString(),
                    quest_id = commonQuestInfo.id,
                    user_id = User.userInfo.id,
                    reward_amount = rewardAmount
                )
            )
        }
    } else if (sessionsAfterComplete > deepFocus.minWorkSessions) {
        // Reward for extra session
        User.addCoins(deepFocus.reward_per_extra_session)
    }



    val isQuestFullyComplete = sessionsAfterComplete >= deepFocus.maxWorkSessions

    if (isQuestFullyComplete) {
        // Quest is fully done, mark as complete and reset for the next time.
        commonQuestInfo.last_completed_on = getCurrentDate()
        commonQuestInfo.last_completed_at = System.currentTimeMillis()
        isQuestComplete.value = true
        // The quest cycle is complete. The state will be reset after the session review.
    }

    // Set break duration for the upcoming break
    if (deepFocus.long_break_after_sessions > 0 && sessionsAfterComplete % deepFocus.long_break_after_sessions == 0) {
        // Time for a long break
        commonQuestInfo.break_duration_minutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.longBreakDurationInMillis).toInt()
    } else {
        // Regular break
        commonQuestInfo.break_duration_minutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.breakDurationInMillis).toInt()
    }

    // Update quest state in DB after every session.
    commonQuestInfo.quest_json = json.encodeToString(deepFocus)
    commonQuestInfo.synced = false
    commonQuestInfo.last_updated = System.currentTimeMillis()
    commonQuestInfo.quest_started_at = 0
    commonQuestInfo.last_completed_at = System.currentTimeMillis() // Signal break start

    scope.launch {
        Log.d("DeepFocusQuestView", "Saving Deep Focus session to local DB for quest: ${commonQuestInfo.id}")
        dao.upsertQuest(commonQuestInfo)
    }

    // Stop the foreground service and timer after each session.
    sendRefreshRequest(context, INTENT_ACTION_STOP_DEEP_FOCUS)
    ServiceInfo.deepFocus.isRunning = false
}

    fun launchQuest(topic: String) {
        val isRegularBlock = deepFocus.completedWorkSessions < deepFocus.minWorkSessions
        deepFocus = if (isRegularBlock) {
            deepFocus.copy(currentRegularTopic = topic)
        } else {
            deepFocus.copy(currentExtraTopic = topic)
        }
        sessionStartTime = System.currentTimeMillis()
        val durationInMinutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.nextFocusDurationInMillis)
        val breakInMinutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.breakDurationInMillis)

        commonQuestInfo.quest_duration_minutes = durationInMinutes.toInt()
        commonQuestInfo.break_duration_minutes = breakInMinutes.toInt()
        commonQuestInfo.quest_started_at = System.currentTimeMillis()
        commonQuestInfo.last_completed_at = 0 // Stop break timer
        commonQuestInfo.last_updated = System.currentTimeMillis()
        commonQuestInfo.synced = false

        scope.launch {
            dao.upsertQuest(commonQuestInfo)
        }

        val timerServiceIntent = Intent(context, TimerService::class.java)
        context.startService(timerServiceIntent)

        if (!ServiceInfo.isUsingAccessibilityService && ServiceInfo.appBlockerService == null) {
            startForegroundService(context, Intent(context, AppBlockerService::class.java))
        }
        ServiceInfo.deepFocus.isRunning = true
        ServiceInfo.deepFocus.exceptionApps = deepFocus.unrestrictedApps.toHashSet()
        val intent = Intent(INTENT_ACTION_START_DEEP_FOCUS)
        intent.putStringArrayListExtra("exception", deepFocus.unrestrictedApps.toCollection(ArrayList()))
        context.sendBroadcast(intent)
    }

    fun startQuest() {
        val sessions = deepFocus.completedWorkSessions
        val isRegularBlock = sessions < deepFocus.minWorkSessions

        val topicToLaunch: String? = if (isRegularBlock) {
            deepFocus.currentRegularTopic
        } else {
            deepFocus.currentExtraTopic
        }

        if (topicToLaunch == null) {
            showStudyTopicDialog.value = true
        } else {
            launchQuest(topicToLaunch)
        }
    }

    LaunchedEffect(Unit) {
        timerViewModel.questFinishedEvent.collect { questId ->
            if (questId == commonQuestInfo.id) {
                onQuestComplete()
            }
        }
    }

    // Prevent back navigation when quest is running
    BackHandler(isQuestRunning) {}

    if (showStudyTopicDialog.value) {
        StudyTopicDialog(
            onDismiss = { showStudyTopicDialog.value = false },
            onConfirm = { topic ->
                launchQuest(topic)
            }
        )
    }

    // Convert progress state to MutableState<Float> for BaseQuestView
    val progressState = remember(progress) { mutableFloatStateOf(progress) }

    if (showQuestCompletionReviewDialog.value) {
        QuestCompletionReviewDialog(
            onDismiss = { showQuestCompletionReviewDialog.value = false },
            onConfirm = { difficulty, mood ->
                scope.launch {
                    // Update local logs and mark them for re-sync
                    deepFocusSessionLogDao.updatePendingLogsForQuest(commonQuestInfo.id, difficulty, mood)

                    // Enqueue a background sync job
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                    val syncWorkRequest = OneTimeWorkRequestBuilder<neth.iecal.questphone.workers.SyncWorker>()
                        .setConstraints(constraints)
                        .build()
                    WorkManager.getInstance(context).enqueue(syncWorkRequest)
                }
                showQuestCompletionReviewDialog.value = false
            }
        )
    }

    val showQuestCompletionDialogIfNeeded = {
        val sessions = deepFocus.completedWorkSessions
        // Special case: if the quest is fully complete, the session counter resets to 0.
        // In this case, we check against maxWorkSessions.
        val sessionsToCheck = if (sessions == 0 && isQuestComplete.value) deepFocus.maxWorkSessions else sessions

        if (sessionsToCheck == deepFocus.minWorkSessions || (deepFocus.maxWorkSessions > deepFocus.minWorkSessions && sessionsToCheck == deepFocus.maxWorkSessions)) {
            showQuestCompletionReviewDialog.value = true
        }
    }

        if (showSessionReviewDialog.value) {
        val sessionLogDao = QuestDatabaseProvider.getInstance(context).deepFocusSessionLogDao()
        val supabaseSyncService = SupabaseSyncService(context)
                SessionReviewDialog(
            onDismiss = {
                showSessionReviewDialog.value = false
                showQuestCompletionDialogIfNeeded()
            },
            onConfirm = { concentration, productivity, wordsStudied ->
                scope.launch {
                    val endTime = System.currentTimeMillis()
                    val sessionNumber = sessionNumberForReview.value
                    val sessionType = if (sessionNumber <= deepFocus.minWorkSessions) "regular" else "extra"

                    val newLog = DeepFocusSessionLog(
                        questId = commonQuestInfo.id,
                        questName = commonQuestInfo.title,
                        sessionStartTime = sessionStartTime,
                        sessionDuration = if (sessionStartTime > 0) endTime - sessionStartTime else 0,
                        timestamp = endTime,
                        sessionNumber = sessionNumber,
                        sessionType = sessionType,
                        studyTopic = if (sessionNumber <= deepFocus.minWorkSessions) deepFocus.currentRegularTopic else deepFocus.currentExtraTopic,
                        concentration = concentration,
                        productivity = productivity,
                        wordsStudied = wordsStudied
                    )

                    Log.d("DeepFocusQuestView", "Saving Deep Focus session log to local DB: ${newLog.client_uuid}")
                    deepFocusSessionLogDao.insert(newLog)

                    Log.d("DeepFocusQuestView", "Syncing Deep Focus session log to Supabase: ${newLog.client_uuid}")
                    supabaseSyncService.syncDeepFocusLog(newLog)

                    val justCompletedSessions = deepFocus.completedWorkSessions
                    if (justCompletedSessions >= deepFocus.maxWorkSessions) {
                        // This was the last session, reset the quest topics and session count for the next cycle
                        deepFocus = deepFocus.copy(completedWorkSessions = 0, currentRegularTopic = null, currentExtraTopic = null)
                    }
                }
                showSessionReviewDialog.value = false
                showQuestCompletionDialogIfNeeded()
            }
        )
    }

    BaseQuestView(
        hideStartQuestBtn = isQuestComplete.value || isQuestRunning || isFailed.value || !isInTimeRange.value,
        progress = progressState,
        isFailed = isFailed,
        onQuestStarted = {
            startQuest()
        },
        onQuestCompleted = { onQuestComplete() },
        isQuestCompleted = isQuestComplete,
        questStartComponent = null
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = commonQuestInfo.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                fontFamily = JetBrainsMonoFont,
                modifier = Modifier.padding(top = 40.dp)
            )

            val rewardText = if (commonQuestInfo.reward_min == commonQuestInfo.reward_max) {
                "${commonQuestInfo.reward_min} coins"
            } else {
                "${commonQuestInfo.reward_min}-${commonQuestInfo.reward_max} coins"
            }
            Text(
                text = (if(isQuestComplete.value) "Reward" else "Next Reward") + ": $rewardText + ${
                    xpToRewardForQuest(
                        userInfo.level
                    )
                } xp",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
            )

            if(!isInTimeRange.value){
                Text(
                    text = "Time: ${formatHour(commonQuestInfo.time_range[0])} to ${formatHour(commonQuestInfo.time_range[1])}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )
            }
            // Show remaining time
            if (isQuestRunning && progress < 1f) {
                Text(
                    text = "Remaining: $timerText",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = if (!isQuestComplete.value) "Duration: ${duration / 60_000}m" else "Next Duration: ${duration / 60_000}m",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                modifier = Modifier.padding(top = 32.dp)
            )

            if (deepFocus.minWorkSessions > 1) {
                Text(
                    text = "Sessions: ${deepFocus.completedWorkSessions} / ${deepFocus.minWorkSessions}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..deepFocus.maxWorkSessions).forEach { sessionNumber ->
                        val isCompleted = sessionNumber <= deepFocus.completedWorkSessions
                        val isOptional = sessionNumber > deepFocus.minWorkSessions
                        val isClickable = (sessionNumber == deepFocus.completedWorkSessions + 1) && !isQuestRunning

                        val color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            isOptional -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable(enabled = isClickable) { startQuest() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sessionNumber.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            val pm = context.packageManager
            val apps = deepFocus.unrestrictedApps.mapNotNull { packageName ->
                try {
                    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() to packageName
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Unrestricted Apps: ",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                )
                apps.forEach { (appName, packageName) ->
                    Text(
                        text = "$appName, ",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                        modifier = Modifier
                            .clickable {
                                val intent = pm.getLaunchIntentForPackage(packageName)
                                intent?.let { context.startActivity(it) }
                            }
                    )
                }
            }
            MdPad(commonQuestInfo)
        }

    }
}
