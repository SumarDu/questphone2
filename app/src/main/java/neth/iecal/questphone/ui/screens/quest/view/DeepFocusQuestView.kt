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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import java.util.UUID
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
import neth.iecal.questphone.ui.screens.quest.logQuestCompletionWithCoins
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
import neth.iecal.questphone.utils.formatTimeMinutes
import neth.iecal.questphone.utils.toMinutesRange
import neth.iecal.questphone.utils.isAllDayRange
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.json
import neth.iecal.questphone.utils.sendRefreshRequest

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
    val showConcentrationDropReasonDialog = remember { mutableStateOf(false) }
    val concentrationDropReason = remember { mutableStateOf("") }
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

    // Track rewarded sessions to prevent duplicate rewards
    val rewardedSessions = remember { mutableStateOf(setOf<Int>()) }
    
    // Track if we've processed completion for the current session
    val hasProcessedCompletion = remember { mutableStateOf(false)}
    // Flag to indicate that a session has JUST completed, to gate side-effects
    val justCompletedSession = remember { mutableStateOf(false) }
    // Track overdue state that requires explicit confirmation to grant reward and start break
    val overduePending = remember { mutableStateOf(false) }
    // Capture reliable start time of the just-finished session for logging
    var completedSessionStartTime by remember { mutableStateOf(0L) }

    // AI Photo Proof integration (reuse existing `context` declared earlier)
    val settingsRepository = remember { neth.iecal.questphone.data.settings.SettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState()
    val pendingPostProofAction = remember { mutableStateOf<(() -> Unit)?>(null) }
    var isPhotoProofLoading by remember { mutableStateOf(false) }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                isPhotoProofLoading = true
                scope.launch {
                    val apiKey = settings.geminiApiKey
                    if (apiKey.isNullOrBlank()) {
                        android.widget.Toast.makeText(context, "Gemini API Key is not set", android.widget.Toast.LENGTH_LONG).show()
                        isPhotoProofLoading = false
                        return@launch
                    }
                    val (isMatch, _) = neth.iecal.questphone.data.ai.GeminiPro.verifyImage(
                        commonQuestInfo.ai_photo_proof_description,
                        apiKey,
                        bitmap
                    )
                    if (isMatch) {
                        pendingPostProofAction.value?.invoke()
                    } else {
                        android.widget.Toast.makeText(context, "Photo does not match the required description.", android.widget.Toast.LENGTH_LONG).show()
                    }
                    isPhotoProofLoading = false
                    pendingPostProofAction.value = null
                }
            }
        }
    )
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                android.widget.Toast.makeText(context, "Camera permission is required for AI Photo Proof", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    )
    
    // Non-Composable function to handle quest completion logic
    fun handleQuestCompletion() {
        if (hasProcessedCompletion.value) return
        hasProcessedCompletion.value = true
        // Mark that we have just completed a session so LaunchedEffect can react once
        justCompletedSession.value = true
        // Snapshot session start time for logging before any state resets
        completedSessionStartTime = if (commonQuestInfo.quest_started_at > 0) {
            commonQuestInfo.quest_started_at
        } else {
            sessionStartTime
        }
        
        // Increment session count and update next focus duration
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
        // Show review dialog immediately only for regular sessions (before minimum)
        // For minimum and extra sessions, we defer showing until user presses 'Complete session'
        if (newCompletedWorkSessions < deepFocus.minWorkSessions) {
            showSessionReviewDialog.value = true
        }
    }
    // Use LaunchedEffect to handle side effects when quest completes
    LaunchedEffect(deepFocus.completedWorkSessions) {
        val sessionsAfterComplete = deepFocus.completedWorkSessions
        if (sessionsAfterComplete == 0) return@LaunchedEffect

        if (sessionsAfterComplete >= deepFocus.minWorkSessions) {
            // Overdue state must be shown even after screen re-entry if not yet confirmed
            // Enter overdue for every session at or beyond minimum, but only if we haven't confirmed yet for this session
            if (commonQuestInfo.last_completed_at == 0L) {
                overduePending.value = true

                // Persist updated quest state (keep quest_started_at to let TimerService show OVERTIME)
                commonQuestInfo.quest_json = json.encodeToString(deepFocus)
                commonQuestInfo.synced = false
                commonQuestInfo.last_updated = System.currentTimeMillis()

                scope.launch {
                    Log.d("DeepFocusQuestView", "Saving Deep Focus session (pending overdue) to local DB for quest: ${commonQuestInfo.id}")
                    dao.upsertQuest(commonQuestInfo)
                }
                // Stop service once when entering overdue
                sendRefreshRequest(context, INTENT_ACTION_STOP_DEEP_FOCUS)
                ServiceInfo.deepFocus.isRunning = false
            } else {
                // If last_completed_at already set, we've already confirmed and started break for this session
                overduePending.value = false
            }
        } else if (sessionsAfterComplete < deepFocus.minWorkSessions) {
            // Only run normal break flow immediately after a session completion
            if (!justCompletedSession.value) return@LaunchedEffect
            // Normal flow before reaching minimum: start a regular break without rewards
            if (deepFocus.long_break_after_sessions > 0 && sessionsAfterComplete % deepFocus.long_break_after_sessions == 0) {
                commonQuestInfo.break_duration_minutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.longBreakDurationInMillis).toInt()
            } else {
                commonQuestInfo.break_duration_minutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.breakDurationInMillis).toInt()
            }

            // Transition to break only once per session: act only if quest was running
            if (commonQuestInfo.quest_started_at > 0L) {
                commonQuestInfo.last_completed_at = System.currentTimeMillis()
                commonQuestInfo.quest_started_at = 0
                // Stop service once when transitioning to break
                sendRefreshRequest(context, INTENT_ACTION_STOP_DEEP_FOCUS)
                ServiceInfo.deepFocus.isRunning = false
            }
            commonQuestInfo.quest_json = json.encodeToString(deepFocus)
            commonQuestInfo.synced = false
            commonQuestInfo.last_updated = System.currentTimeMillis()

            dao.upsertQuest(commonQuestInfo)
            overduePending.value = false
        }
        // Reset the one-shot completion flag after handling side-effects
        justCompletedSession.value = false
    }

    fun launchQuest(topic: String) {
        // Reset completion guard for the new session so it can be counted on finish
        hasProcessedCompletion.value = false
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

        // Set flags BEFORE starting the app blocker service to avoid race on first launch
        ServiceInfo.deepFocus.isRunning = true
        ServiceInfo.deepFocus.exceptionApps = deepFocus.unrestrictedApps.toHashSet()

        if (!ServiceInfo.isUsingAccessibilityService && ServiceInfo.appBlockerService == null) {
            // Start service with action and extras so it can initialize immediately (no broadcast race)
            val appBlockerIntent = Intent(context, AppBlockerService::class.java).apply {
                action = INTENT_ACTION_START_DEEP_FOCUS
                putStringArrayListExtra("exception", deepFocus.unrestrictedApps.toCollection(ArrayList()))
            }
            startForegroundService(context, appBlockerIntent)
        } else {
            // If service already running, send broadcast to refresh
            val intent = Intent(INTENT_ACTION_START_DEEP_FOCUS)
            intent.putStringArrayListExtra("exception", deepFocus.unrestrictedApps.toCollection(ArrayList()))
            context.sendBroadcast(intent)
        }
    }

    fun startQuest() {
        // Do not allow starting a new session if the quest is already fully completed
        if (isQuestComplete.value) return
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
        // If process/view was recreated during a running session, restore start time from persisted state
        if (sessionStartTime == 0L && commonQuestInfo.quest_started_at > 0L) {
            sessionStartTime = commonQuestInfo.quest_started_at
        }
        timerViewModel.questFinishedEvent.collect { questId ->
            if (questId == commonQuestInfo.id) {
                handleQuestCompletion()
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
        // Decide if the "Later" button should be shown: only for extra sessions except the last one
        val sessionsNow = deepFocus.completedWorkSessions
        val effectiveSessions = if (sessionsNow == 0 && isQuestComplete.value) deepFocus.maxWorkSessions else sessionsNow
        val showLaterButton = effectiveSessions > deepFocus.minWorkSessions && effectiveSessions < deepFocus.maxWorkSessions

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
            },
            showLater = showLaterButton
        )
    }

    var sessionReviewConcentration by remember { mutableStateOf(0) }
    var sessionReviewProductivity by remember { mutableStateOf(0) }
    var sessionReviewWordsStudied by remember { mutableStateOf(0) }

    val showQuestCompletionDialogIfNeeded = {
        val sessions = deepFocus.completedWorkSessions
        // Special case: if the quest is fully complete, the session counter resets to 0.
        // In this case, we check against maxWorkSessions.
        val sessionsToCheck = if (sessions == 0 && isQuestComplete.value) deepFocus.maxWorkSessions else sessions

        // Show survey after:
        // - completing minimum sessions
        // - each extra session (sessions > minWorkSessions) up to maxWorkSessions
        if (
            sessionsToCheck == deepFocus.minWorkSessions ||
            (sessionsToCheck > deepFocus.minWorkSessions && sessionsToCheck <= deepFocus.maxWorkSessions)
        ) {
            showQuestCompletionReviewDialog.value = true
        }
    }

        val sessionLogDao = QuestDatabaseProvider.getInstance(context).deepFocusSessionLogDao()
        val supabaseSyncService = SupabaseSyncService(context)

        if (showSessionReviewDialog.value) {
                SessionReviewDialog(
            onDismiss = {
                showSessionReviewDialog.value = false
                showQuestCompletionDialogIfNeeded()
            },
            onConfirm = { concentration, productivity, wordsStudied ->
                if (concentration < 5) {
                    // Store the results for later use
                    sessionReviewConcentration = concentration
                    sessionReviewProductivity = productivity
                    sessionReviewWordsStudied = wordsStudied
                    showSessionReviewDialog.value = false
                    showConcentrationDropReasonDialog.value = true
                } else {
                    scope.launch {
                        val endTime = System.currentTimeMillis()
                        val sessionNumber = sessionNumberForReview.value
                        val sessionType = if (sessionNumber <= deepFocus.minWorkSessions) "regular" else "extra"

                        val plannedDuration = kotlin.runCatching { TimeUnit.MINUTES.toMillis(commonQuestInfo.quest_duration_minutes.toLong()) }.getOrDefault(0L)
                        val inferredStart = if (plannedDuration > 0) endTime - plannedDuration else 0L
                        val startMs = when {
                            completedSessionStartTime > 0L -> completedSessionStartTime
                            sessionStartTime > 0L -> sessionStartTime
                            commonQuestInfo.quest_started_at > 0L -> commonQuestInfo.quest_started_at
                            inferredStart > 0L -> inferredStart
                            else -> endTime
                        }
                        val durationMs = if (startMs in 1 until endTime) endTime - startMs else 0L
                        val newLog = DeepFocusSessionLog(
                            questId = commonQuestInfo.id,
                            questName = commonQuestInfo.title,
                            sessionStartTime = startMs,
                            sessionDuration = durationMs,
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

                        // Clear quest_started_at immediately after local log so TimerService can switch to BREAK promptly
                        if (commonQuestInfo.quest_started_at > 0L) {
                            commonQuestInfo.quest_started_at = 0
                            commonQuestInfo.last_updated = System.currentTimeMillis()
                            commonQuestInfo.synced = false
                            dao.upsertQuest(commonQuestInfo)
                        }

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
            }
        )
        }

        if (showConcentrationDropReasonDialog.value) {
            ConcentrationDropReasonDialog(
                onDismiss = {
                    showConcentrationDropReasonDialog.value = false
                    showQuestCompletionDialogIfNeeded()
                },
                onConfirm = { reason ->
                    scope.launch {
                        val endTime = System.currentTimeMillis()
                        val sessionNumber = sessionNumberForReview.value
                        val sessionType = if (sessionNumber <= deepFocus.minWorkSessions) "regular" else "extra"

                        val plannedDuration2 = kotlin.runCatching { TimeUnit.MINUTES.toMillis(commonQuestInfo.quest_duration_minutes.toLong()) }.getOrDefault(0L)
                        val inferredStart2 = if (plannedDuration2 > 0) endTime - plannedDuration2 else 0L
                        val startMs2 = when {
                            completedSessionStartTime > 0L -> completedSessionStartTime
                            sessionStartTime > 0L -> sessionStartTime
                            commonQuestInfo.quest_started_at > 0L -> commonQuestInfo.quest_started_at
                            inferredStart2 > 0L -> inferredStart2
                            else -> endTime
                        }
                        val durationMs2 = if (startMs2 in 1 until endTime) endTime - startMs2 else 0L
                        val newLog = DeepFocusSessionLog(
                            questId = commonQuestInfo.id,
                            questName = commonQuestInfo.title,
                            sessionStartTime = startMs2,
                            sessionDuration = durationMs2,
                            timestamp = endTime,
                            sessionNumber = sessionNumber,
                            sessionType = sessionType,
                            studyTopic = if (sessionNumber <= deepFocus.minWorkSessions) deepFocus.currentRegularTopic else deepFocus.currentExtraTopic,
                            concentration = sessionReviewConcentration,
                            productivity = sessionReviewProductivity,
                            wordsStudied = sessionReviewWordsStudied,
                            concentrationDropReason = reason
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
                    showConcentrationDropReasonDialog.value = false
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
        onQuestCompleted = { handleQuestCompletion() },
        isQuestCompleted = isQuestComplete,
        questStartComponent = if (overduePending.value && deepFocus.completedWorkSessions >= deepFocus.minWorkSessions) {
            {
                Button(
                    onClick = {
                        scope.launch {
                            // Guard: avoid double-processing if already confirmed
                            if (commonQuestInfo.last_completed_at != 0L) {
                                overduePending.value = false
                                return@launch
                            }
                            val sessionsAfterComplete = deepFocus.completedWorkSessions
                            // Grant reward depending on session stage
                            val rewardAmount = if (sessionsAfterComplete == deepFocus.minWorkSessions) {
                                calculateQuestReward(commonQuestInfo)
                            } else {
                                deepFocus.reward_per_extra_session
                            }
                            // If AI Photo Proof enabled, gate the rest of the flow behind successful proof
                            if (commonQuestInfo.ai_photo_proof) {
                                pendingPostProofAction.value = {
                                    scope.launch {
                                        checkForRewards(commonQuestInfo, rewardAmount)
                                        val statsDao = StatsDatabaseProvider.getInstance(context).statsDao()
                                        statsDao.upsertStats(
                                            StatsInfo(
                                                id = UUID.randomUUID().toString(),
                                                quest_id = commonQuestInfo.id,
                                                user_id = User.userInfo.id,
                                                reward_amount = rewardAmount
                                            )
                                        )
                                        neth.iecal.questphone.ui.screens.quest.RewardDialogInfo.lastProcessedSessionForQuest[commonQuestInfo.id] = sessionsAfterComplete

                                        // Set break duration
                                        if (deepFocus.long_break_after_sessions > 0 && sessionsAfterComplete % deepFocus.long_break_after_sessions == 0) {
                                            commonQuestInfo.break_duration_minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(deepFocus.longBreakDurationInMillis).toInt()
                                        } else {
                                            commonQuestInfo.break_duration_minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(deepFocus.breakDurationInMillis).toInt()
                                        }

                                        // Mark quest done for the day if max sessions reached
                                        val isQuestFullyComplete = sessionsAfterComplete >= deepFocus.maxWorkSessions
                                        if (isQuestFullyComplete) {
                                            commonQuestInfo.last_completed_on = getCurrentDate()
                                            isQuestComplete.value = true
                                        }

                                        // Start break and persist (clear quest_started_at now so TimerService enters BREAK)
                                        commonQuestInfo.last_completed_at = System.currentTimeMillis()
                                        commonQuestInfo.quest_started_at = 0
                                        commonQuestInfo.quest_json = json.encodeToString(deepFocus)
                                        commonQuestInfo.synced = false
                                        commonQuestInfo.last_updated = System.currentTimeMillis()

                                        dao.upsertQuest(commonQuestInfo)
                                        overduePending.value = false

                                        // Trigger review flows now that user confirmed completion
                                        sessionNumberForReview.value = sessionsAfterComplete
                                        showSessionReviewDialog.value = true
                                    }
                                }
                                // Request camera or permission
                                when (android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) -> {
                                        cameraLauncher.launch(null)
                                    }
                                    else -> {
                                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                                return@launch
                            }
                            checkForRewards(commonQuestInfo, rewardAmount)
                            val statsDao = StatsDatabaseProvider.getInstance(context).statsDao()
                            statsDao.upsertStats(
                                StatsInfo(
                                    id = UUID.randomUUID().toString(),
                                    quest_id = commonQuestInfo.id,
                                    user_id = User.userInfo.id,
                                    reward_amount = rewardAmount
                                )
                            )
                            neth.iecal.questphone.ui.screens.quest.RewardDialogInfo.lastProcessedSessionForQuest[commonQuestInfo.id] = sessionsAfterComplete

                            // Set break duration
                            if (deepFocus.long_break_after_sessions > 0 && sessionsAfterComplete % deepFocus.long_break_after_sessions == 0) {
                                commonQuestInfo.break_duration_minutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.longBreakDurationInMillis).toInt()
                            } else {
                                commonQuestInfo.break_duration_minutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.breakDurationInMillis).toInt()
                            }

                            // Mark quest done for the day if max sessions reached
                            val isQuestFullyComplete = sessionsAfterComplete >= deepFocus.maxWorkSessions
                            if (isQuestFullyComplete) {
                                commonQuestInfo.last_completed_on = getCurrentDate()
                                isQuestComplete.value = true
                            }

                            // Start break and persist (clear quest_started_at now so TimerService enters BREAK)
                            commonQuestInfo.last_completed_at = System.currentTimeMillis()
                            commonQuestInfo.quest_started_at = 0
                            commonQuestInfo.quest_json = json.encodeToString(deepFocus)
                            commonQuestInfo.synced = false
                            commonQuestInfo.last_updated = System.currentTimeMillis()

                            dao.upsertQuest(commonQuestInfo)
                            overduePending.value = false

                            // Trigger review flows now that user confirmed completion
                            sessionNumberForReview.value = sessionsAfterComplete
                            showSessionReviewDialog.value = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Complete session")
                }
            }
        } else null
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
                    text = run {
                        val (s, e) = toMinutesRange(commonQuestInfo.time_range)
                        if (isAllDayRange(commonQuestInfo.time_range)) "Time: All day"
                        else "Time: ${formatTimeMinutes(s)} to ${formatTimeMinutes(e)}"
                    },
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

            // Overdue confirmation button moved to Start area via questStartComponent

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
                        val isClickable = (sessionNumber == deepFocus.completedWorkSessions + 1) && !isQuestRunning && !overduePending.value && !isQuestComplete.value

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
