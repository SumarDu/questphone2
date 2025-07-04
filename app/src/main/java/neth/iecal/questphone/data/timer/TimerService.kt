package neth.iecal.questphone.data.timer

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.utils.NotificationHelper
import neth.iecal.questphone.utils.getCurrentDate
import java.time.Duration
import java.util.concurrent.TimeUnit

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var questDao: neth.iecal.questphone.data.quest.QuestDao
    private var lastNotifiedOvertimeMinute = -1L

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
            while (isActive) {
                updateTimerState()
                val now = System.currentTimeMillis()
                val delayMillis = 1000 - (now % 1000)
                delay(delayMillis)
            }
        }

        return START_STICKY
    }

    private suspend fun updateTimerState() {
        val allQuests = questDao.getAllQuestsSuspend()
        val now = System.currentTimeMillis()
        val today = getCurrentDate()
        val previousMode = _timerState.value.mode

        val activeQuest = allQuests.firstOrNull { it.quest_started_at > 0 && it.last_completed_on != today }

        if (activeQuest != null && activeQuest.quest_duration_minutes > 0) {
            val questEndTime = activeQuest.quest_started_at + TimeUnit.MINUTES.toMillis(activeQuest.quest_duration_minutes.toLong())

            if (now < questEndTime) {
                val remaining = Duration.ofMillis(questEndTime - now)
                _timerState.value = TimerState(TimerMode.QUEST_COUNTDOWN, remaining, activeQuest.id)
            } else {
                if (previousMode == TimerMode.QUEST_COUNTDOWN) {
                    notificationHelper.showQuestAlertNotification("Quest Complete!", "Great job!")
                }
                val overtimeDuration = Duration.ofMillis(now - questEndTime)
                _timerState.value = TimerState(TimerMode.OVERTIME, overtimeDuration, activeQuest.id)

                val overtimeMinutes = overtimeDuration.toMinutes()
                if (overtimeMinutes > 0 && overtimeMinutes % 5 == 0L && overtimeMinutes != lastNotifiedOvertimeMinute) {
                    notificationHelper.showQuestAlertNotification("Quest Overdue", "You've been in overtime for $overtimeMinutes minutes.")
                    lastNotifiedOvertimeMinute = overtimeMinutes
                }
            }
            updateNotification()
            return
        }

        val recentlyCompleted = allQuests
            .filter { it.last_completed_on == today && it.break_duration_minutes > 0 && it.last_completed_at > 0 }
            .maxByOrNull { it.last_completed_at }

        if (recentlyCompleted != null) {
            val breakEndTime = recentlyCompleted.last_completed_at + TimeUnit.MINUTES.toMillis(recentlyCompleted.break_duration_minutes.toLong())

            if (now < breakEndTime) {
                val remaining = Duration.ofMillis(breakEndTime - now)
                _timerState.value = TimerState(TimerMode.BREAK, remaining, recentlyCompleted.id)
            } else {
                if (previousMode == TimerMode.BREAK) {
                    notificationHelper.showQuestAlertNotification("Break's Over!", "Time to start a new quest.")
                }
                _timerState.value = TimerState(TimerMode.INACTIVE)
            }
            updateNotification()
            return
        }

        _timerState.value = TimerState()
        updateNotification()
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
        val positive = String.format(
            "%02d:%02d",
            (absSeconds % 3600) / 60,
            absSeconds % 60
        )
        return if (seconds < 0) "-$positive" else positive
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private val _timerState = MutableStateFlow(TimerState())
        val timerState = _timerState.asStateFlow()
    }
}
