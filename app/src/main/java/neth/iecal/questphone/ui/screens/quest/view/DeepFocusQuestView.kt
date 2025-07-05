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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import neth.iecal.questphone.ui.screens.launcher.TimerViewModel
import java.util.concurrent.TimeUnit
import androidx.compose.ui.Modifier
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
import neth.iecal.questphone.data.game.getUserInfo
import neth.iecal.questphone.data.game.xpToRewardForQuest
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.focus.DeepFocus
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsInfo
import neth.iecal.questphone.services.AppBlockerService
import neth.iecal.questphone.services.INTENT_ACTION_START_DEEP_FOCUS
import neth.iecal.questphone.services.INTENT_ACTION_STOP_DEEP_FOCUS
import neth.iecal.questphone.services.ServiceInfo
import neth.iecal.questphone.ui.screens.quest.checkForRewards
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
    val deepFocus = json.decodeFromString<DeepFocus>(commonQuestInfo.quest_json)
    val duration = deepFocus.nextFocusDurationInMillis
    val isInTimeRange = remember { mutableStateOf(QuestHelper.Companion.isInTimeRange(commonQuestInfo)) }

    val timerViewModel: TimerViewModel = viewModel()
    val timerMode by timerViewModel.timerMode.collectAsState()
    val timerText by timerViewModel.timerText.collectAsState()
    val timerState by TimerService.timerState.collectAsState()

    val isQuestRunning by remember(timerMode, timerState) {
        mutableStateOf(timerMode == TimerMode.QUEST_COUNTDOWN && timerState.activeQuestId == commonQuestInfo.id)
    }

    val isQuestComplete = remember {
        mutableStateOf(commonQuestInfo.last_completed_on == getCurrentDate())
    }

    val isFailed = remember { mutableStateOf(QuestHelper(context).isOver(commonQuestInfo)) }

    val dao = QuestDatabaseProvider.getInstance(context).questDao()
    val scope = rememberCoroutineScope()

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
        deepFocus.incrementTime()
        commonQuestInfo.last_completed_on = getCurrentDate()
        commonQuestInfo.quest_json = json.encodeToString(deepFocus)
        commonQuestInfo.synced = false
        commonQuestInfo.last_updated = System.currentTimeMillis()
        commonQuestInfo.quest_started_at = 0
        commonQuestInfo.last_completed_at = System.currentTimeMillis()

        scope.launch {
            dao.upsertQuest(commonQuestInfo)

            val statsDao = StatsDatabaseProvider.getInstance(context).statsDao()
            statsDao.upsertStats(
                StatsInfo(
                    id = UUID.randomUUID().toString(),
                    quest_id = commonQuestInfo.id,
                    user_id = User.userInfo.id,
                )
            )
        }

        checkForRewards(commonQuestInfo)
        sendRefreshRequest(context, INTENT_ACTION_STOP_DEEP_FOCUS)

        ServiceInfo.deepFocus.isRunning = false
        isQuestComplete.value = true
    }

    fun startQuest() {
        val durationInMinutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.nextFocusDurationInMillis)
        val breakInMinutes = TimeUnit.MILLISECONDS.toMinutes(deepFocus.breakDurationInMillis)

        commonQuestInfo.quest_duration_minutes = durationInMinutes.toInt()
        commonQuestInfo.break_duration_minutes = breakInMinutes.toInt()
        commonQuestInfo.quest_started_at = System.currentTimeMillis()
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

    LaunchedEffect(Unit) {
        timerViewModel.questFinishedEvent.collect { questId ->
            if (questId == commonQuestInfo.id) {
                onQuestComplete()
            }
        }
    }

    // Prevent back navigation when quest is running
    BackHandler(isQuestRunning) {}

    // Convert progress state to MutableState<Float> for BaseQuestView
    val progressState = remember(progress) { mutableFloatStateOf(progress) }

    BaseQuestView(
        hideStartQuestBtn = isQuestComplete.value || isQuestRunning || isFailed.value || !isInTimeRange.value,
        progress = progressState,
        isFailed = isFailed,
        onQuestStarted = {
            // Start the quest immediately - this is called when button is pressed
            startQuest()
        },
         onQuestCompleted = {onQuestComplete()},
        isQuestCompleted = isQuestComplete) {

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
