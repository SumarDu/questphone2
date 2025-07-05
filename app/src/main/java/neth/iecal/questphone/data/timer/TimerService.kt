package neth.iecal.questphone.data.timer

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.utils.NotificationHelper
import neth.iecal.questphone.data.timer.AlarmHelper
import neth.iecal.questphone.utils.getCurrentDate
import java.time.Duration
import java.util.concurrent.TimeUnit

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var questDao: neth.iecal.questphone.data.quest.QuestDao


    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        questDao = QuestDatabaseProvider.getInstance(this).questDao()
        notificationHelper.createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationHelper.buildTimerNotification("Starting timer...")
        startForeground(
            NotificationHelper.TIMER_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        serviceScope.launch {
            // Use a ticker channel for a fixed-rate timer to avoid drift and ensure smooth updates.
            val now = System.currentTimeMillis()
            val initialDelay = 1000 - (now % 1000)
            val tickerChannel = ticker(delayMillis = 1000, initialDelayMillis = initialDelay)
            for (unit in tickerChannel) {
                updateTimerState()
            }
        }

        return START_STICKY
    }

    private suspend fun updateTimerState() {
        val allQuests = questDao.getAllQuestsSuspend()
        val now = System.currentTimeMillis()
        val today = getCurrentDate()
        val previousState = _timerState.value

        val activeQuest = allQuests.firstOrNull { it.quest_started_at > 0 && it.last_completed_on != today }
        val recentlyCompleted = allQuests
            .filter { it.last_completed_on == today && it.break_duration_minutes > 0 && it.last_completed_at > 0 }
            .maxByOrNull { it.last_completed_at }

        var currentStateHandled = false

        // 1. Check for active quest
        if (activeQuest != null && activeQuest.quest_duration_minutes > 0) {
            currentStateHandled = true
            val questEndTime = activeQuest.quest_started_at + TimeUnit.MINUTES.toMillis(activeQuest.quest_duration_minutes.toLong())

            // Cancel alarms for previous quest if different
            if (previousState.activeQuestId != null && previousState.activeQuestId != activeQuest.id) {
                cancelAlarmsForQuest(previousState.activeQuestId!!)
            }

            if (now < questEndTime) {
                // State: QUEST_COUNTDOWN
                _timerState.value = TimerState(TimerMode.QUEST_COUNTDOWN, Duration.ofMillis(questEndTime - now), activeQuest.id)
                if (previousState.mode != TimerMode.QUEST_COUNTDOWN) {
                    // New state, schedule completion alarm
                    AlarmHelper.setAlarm(
                        this, questEndTime, activeQuest.id.hashCode(),
                        AlarmHelper.ALARM_TYPE_QUEST_COMPLETE, activeQuest.id,
                        "Quest Complete!", "Great job!"
                    )
                }
            } else {
                // State: OVERTIME (Quest)
                val overtimeDuration = Duration.ofMillis(now - questEndTime)
                _timerState.value = TimerState(TimerMode.OVERTIME, overtimeDuration, activeQuest.id)
                if (previousState.mode == TimerMode.QUEST_COUNTDOWN) {
                    // Just finished, schedule first overdue alarm
                    AlarmHelper.setAlarm(
                        this, questEndTime + TimeUnit.MINUTES.toMillis(5),
                        activeQuest.id.hashCode() + OVERDUE_QUEST_ALARM_OFFSET,
                        AlarmHelper.ALARM_TYPE_QUEST_OVERDUE, activeQuest.id,
                        "Quest Overdue", "You've been in overtime for 5 minutes.",
                        questEndTime
                    )
                }
            }
        }
        // 2. Check for break
        else if (recentlyCompleted != null) {
            currentStateHandled = true
            val breakEndTime = recentlyCompleted.last_completed_at + TimeUnit.MINUTES.toMillis(recentlyCompleted.break_duration_minutes.toLong())

            // Cancel alarms for previous quest if different
            if (previousState.activeQuestId != null && previousState.activeQuestId != recentlyCompleted.id) {
                cancelAlarmsForQuest(previousState.activeQuestId!!)
            }

            if (now < breakEndTime) {
                // State: BREAK
                _timerState.value = TimerState(TimerMode.BREAK, Duration.ofMillis(breakEndTime - now), recentlyCompleted.id)
                if (previousState.mode != TimerMode.BREAK) {
                    // New state, schedule break over alarm
                    AlarmHelper.setAlarm(
                        this, breakEndTime, recentlyCompleted.id.hashCode(),
                        AlarmHelper.ALARM_TYPE_BREAK_OVER, recentlyCompleted.id,
                        "Break's Over!", "Time to get back to it."
                    )
                }
            } else {
                // State: OVERTIME (Break)
                val overtimeDuration = Duration.ofMillis(now - breakEndTime)
                _timerState.value = TimerState(TimerMode.OVERTIME, overtimeDuration, recentlyCompleted.id)
                if (previousState.mode == TimerMode.BREAK) {
                    // Just finished, schedule first overdue alarm
                    AlarmHelper.setAlarm(
                        this, breakEndTime + TimeUnit.MINUTES.toMillis(5),
                        recentlyCompleted.id.hashCode() + OVERDUE_BREAK_ALARM_OFFSET,
                        AlarmHelper.ALARM_TYPE_BREAK_OVERDUE, recentlyCompleted.id,
                        "Break Overdue", "You've been in overtime for 5 minutes.",
                        breakEndTime
                    )
                }
            }
        }

        // 3. If no active state, go to INACTIVE
        if (!currentStateHandled) {
            if (previousState.activeQuestId != null) {
                cancelAlarmsForQuest(previousState.activeQuestId!!)
            }
            _timerState.value = TimerState()
        }

        updateNotification()
    }

    private fun cancelAlarmsForQuest(questId: String) {
        val questIdHash = questId.hashCode()
        AlarmHelper.cancelAlarm(this, questIdHash)
        AlarmHelper.cancelAlarm(this, questIdHash + OVERDUE_QUEST_ALARM_OFFSET)
        AlarmHelper.cancelAlarm(this, questIdHash + OVERDUE_BREAK_ALARM_OFFSET)
    }

    private fun updateNotification() {
        val state = _timerState.value
        val timerText = formatDuration(state.time)
        val notification = notificationHelper.buildTimerNotification(timerText)
        notificationHelper.notificationManager.notify(NotificationHelper.TIMER_NOTIFICATION_ID, notification)
    }

    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val absSeconds = kotlin.math.abs(seconds)

        val positive = if (absSeconds >= 3600) {
            String.format(
                "%02d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60
            )
        } else {
            String.format(
                "%02d:%02d",
                (absSeconds % 3600) / 60,
                absSeconds % 60
            )
        }

        return if (seconds < 0) "-$positive" else positive
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

        companion object {
        private const val OVERDUE_QUEST_ALARM_OFFSET = 10000 // Offset to avoid collision
        private const val OVERDUE_BREAK_ALARM_OFFSET = 20000 // Offset to avoid collision
        private val _timerState = MutableStateFlow(TimerState())
        val timerState = _timerState.asStateFlow()
    }
}
