package neth.iecal.questphone.data.timer

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import neth.iecal.questphone.data.local.WardenDatabase
import neth.iecal.questphone.data.remote.SupabaseSyncService
import neth.iecal.questphone.data.local.WardenEvent
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
    private lateinit var wardenDao: neth.iecal.questphone.data.local.WardenDao
    private lateinit var supabaseSyncService: SupabaseSyncService
    private var currentWardenEventId: Int? = null
    private var endTimeUpdateJob: Job? = null
    private var stateBeforeUnplannedBreak: TimerState? = null
    private var wardenEventIdBeforeUnplannedBreak: Int? = null
    private var temporaryAddedTimeMillis = 0L
    private var infoModeActive = false
    private var stateBeforeInfoMode: TimerState? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        questDao = QuestDatabaseProvider.getInstance(this).questDao()
        wardenDao = WardenDatabase.getDatabase(this).wardenDao()
        supabaseSyncService = SupabaseSyncService()
        notificationHelper.createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_UNPLANNED_BREAK -> {
                startUnplannedBreak()
                return START_STICKY
            }
            ACTION_STOP_UNPLANNED_BREAK -> {
                stopUnplannedBreak()
                return START_STICKY
            }
            ACTION_COMPLETE_QUEST -> {
                completeQuest()
                return START_STICKY
            }
            ACTION_ADD_TIME -> {
                val timeToAdd = intent.getLongExtra(EXTRA_TIME_TO_ADD, 0)
                addTime(timeToAdd)
                return START_STICKY
            }
            ACTION_TOGGLE_INFO_MODE -> {
                toggleInfoMode()
                return START_STICKY
            }
        }

        val notification = notificationHelper.buildTimerNotification("Starting timer...")
        startForeground(
            NotificationHelper.TIMER_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
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
            .filter { it.last_completed_at > it.quest_started_at && it.break_duration_minutes > 0 }
            .maxByOrNull { it.last_completed_at }

        var currentStateHandled = false

        if (infoModeActive) {
            val referenceState = stateBeforeInfoMode ?: return
            val questId = referenceState.activeQuestId ?: return

            val quest = allQuests.firstOrNull { it.id == questId }
            val recentlyCompletedQuest = allQuests.firstOrNull { it.id == questId && it.last_completed_at > 0 }

            var stillValid = false
            var startTime: Long? = null

            when (referenceState.mode) {
                TimerMode.QUEST_COUNTDOWN -> {
                    val quest = allQuests.firstOrNull { it.id == referenceState.activeQuestId }
                    if (quest != null && quest.last_completed_on != getCurrentDate()) {
                        val questDurationMillis = TimeUnit.MINUTES.toMillis(quest.quest_duration_minutes.toLong())
                        val questEndsAt = quest.quest_started_at + questDurationMillis + temporaryAddedTimeMillis
                        if (now < questEndsAt) {
                            startTime = quest.quest_started_at
                            stillValid = true
                        } else {
                            startTime = null // Quest has ended, invalidate INFO mode
                        }
                    } else {
                        startTime = null
                    }
                }
                TimerMode.BREAK -> {
                    val quest = allQuests.firstOrNull { it.id == referenceState.activeQuestId }
                    if (quest != null) {
                        val breakEndsAt = quest.last_completed_at + TimeUnit.MINUTES.toMillis(quest.break_duration_minutes.toLong())
                        if (now < breakEndsAt) {
                            startTime = quest.last_completed_at
                            stillValid = true
                        } else {
                            startTime = null
                        }
                    } else {
                        startTime = null
                    }
                }
                else -> {
                    startTime = null
                }
            }

            if (stillValid && startTime != null) {
                val elapsed = Duration.ofMillis(now - startTime)
                _timerState.value = referenceState.copy(mode = TimerMode.INFO, time = elapsed)
            } else {
                // The original context is no longer valid, so exit info mode.
                infoModeActive = false
                stateBeforeInfoMode = null
            }
            updateNotification()
            return
        }

        if (activeQuest == null && temporaryAddedTimeMillis != 0L) {
            temporaryAddedTimeMillis = 0L
        }

        if (_timerState.value.mode == TimerMode.UNPLANNED_BREAK) {
            val unplannedBreakEvent = wardenDao.getEventById(currentWardenEventId ?: -1)
            if (unplannedBreakEvent != null) {
                val duration = Duration.ofMillis(now - unplannedBreakEvent.startTime)
                _timerState.value = _timerState.value.copy(time = duration)
            }
            return
        }

        if (previousState.activeQuestId != null && previousState.activeQuestId != activeQuest?.id) {
            cancelAlarmsForQuest(previousState.activeQuestId)
        }

        if (activeQuest != null && activeQuest.quest_duration_minutes > 0) {
            currentStateHandled = true
            val questDurationMillis = TimeUnit.MINUTES.toMillis(activeQuest.quest_duration_minutes.toLong())
            val actualQuestEndsAt = activeQuest.quest_started_at + questDurationMillis + temporaryAddedTimeMillis
            val remaining = actualQuestEndsAt - now

            val needsAlarmUpdate = previousState.questEndsAt != actualQuestEndsAt

            if (now < actualQuestEndsAt) {
                _timerState.value = TimerState(
                    mode = TimerMode.QUEST_COUNTDOWN,
                    time = Duration.ofMillis(remaining),
                    activeQuestId = activeQuest.id,
                    questEndsAt = actualQuestEndsAt
                )

                if (needsAlarmUpdate) {
                    cancelAlarmsForQuest(activeQuest.id)
                    AlarmHelper.setAlarm(
                        context = this,
                        timeInMillis = actualQuestEndsAt,
                        requestCode = activeQuest.id.hashCode(),
                        alarmType = AlarmHelper.ALARM_TYPE_QUEST_COMPLETE,
                        questId = activeQuest.id,
                        title = "Quest Complete!",
                        message = "'${activeQuest.title}' is complete!"
                    )
                }
            } else {
                _timerState.value = TimerState(
                    mode = TimerMode.OVERTIME,
                    time = Duration.ofMillis(-remaining),
                    activeQuestId = activeQuest.id,
                    isBreakOvertime = false,
                    questEndsAt = actualQuestEndsAt
                )

                if (needsAlarmUpdate) {
                    cancelAlarmsForQuest(activeQuest.id)
                    AlarmHelper.setAlarm(
                        context = this,
                        timeInMillis = now + OVERDUE_QUEST_ALARM_OFFSET, // 10 seconds from now
                        requestCode = activeQuest.id.hashCode() + OVERDUE_QUEST_ALARM_OFFSET,
                        alarmType = AlarmHelper.ALARM_TYPE_QUEST_OVERDUE,
                        questId = activeQuest.id,
                        title = "Quest Overdue!",
                        message = "You are running overtime on '${activeQuest.title}'!"
                    )
                }
            }
        } else if (recentlyCompleted != null) {
            currentStateHandled = true
            val breakEndsAt = recentlyCompleted.last_completed_at + TimeUnit.MINUTES.toMillis(recentlyCompleted.break_duration_minutes.toLong())

            var isLocking = false
            if (recentlyCompleted.integration_id == neth.iecal.questphone.data.IntegrationId.DEEP_FOCUS) {
                try {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val deepFocus = json.decodeFromString<neth.iecal.questphone.data.quest.focus.DeepFocus>(recentlyCompleted.quest_json)
                    if (deepFocus.completedWorkSessions < deepFocus.minWorkSessions && deepFocus.completedWorkSessions > 0) {
                        isLocking = true
                    }
                } catch (e: Exception) {
                    // Log error
                }
            }

            if (now < breakEndsAt) {
                val timeUntilBreakEnd = Duration.ofMillis(breakEndsAt - now)
                _timerState.value = TimerState(
                    mode = TimerMode.BREAK, 
                    time = timeUntilBreakEnd, 
                    activeQuestId = recentlyCompleted.id, 
                    isDeepFocusLocking = isLocking
                )

                if (previousState.mode != TimerMode.BREAK) {
                    cancelAlarmsForQuest(recentlyCompleted.id)
                    AlarmHelper.setAlarm(
                        context = this,
                        timeInMillis = breakEndsAt,
                        requestCode = recentlyCompleted.id.hashCode(),
                        alarmType = AlarmHelper.ALARM_TYPE_BREAK_OVER,
                        questId = recentlyCompleted.id,
                        title = "Break Over!",
                        message = "Time to get back to it."
                    )
                }
            } else {
                val overtimeDuration = Duration.ofMillis(now - breakEndsAt)
                _timerState.value = TimerState(
                    mode = TimerMode.OVERTIME, 
                    time = overtimeDuration, 
                    activeQuestId = recentlyCompleted.id, 
                    isBreakOvertime = true, 
                    isDeepFocusLocking = isLocking
                )

                if (previousState.mode != TimerMode.OVERTIME || previousState.isBreakOvertime != true) {
                    cancelAlarmsForQuest(recentlyCompleted.id)
                    AlarmHelper.setAlarm(
                        context = this,
                        timeInMillis = now + OVERDUE_BREAK_ALARM_OFFSET,
                        requestCode = recentlyCompleted.id.hashCode() + OVERDUE_BREAK_ALARM_OFFSET,
                        alarmType = AlarmHelper.ALARM_TYPE_BREAK_OVERDUE,
                        questId = recentlyCompleted.id,
                        title = "Break Overdue!",
                        message = "You're taking too long of a break."
                    )
                }
            }
        }

        if (!currentStateHandled && _timerState.value.mode != TimerMode.INACTIVE) {
            infoModeActive = false // Reset info mode when inactive
            _timerState.value = TimerState(TimerMode.INACTIVE)
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

    private fun startEndTimeUpdater() {
        endTimeUpdateJob?.cancel()
        endTimeUpdateJob = serviceScope.launch {
            while (isActive) {
                delay(30000) // Update every 30 seconds
                currentWardenEventId?.let { eventId ->
                    val currentTime = System.currentTimeMillis()
                    wardenDao.updateEventEndTime(eventId, currentTime)
                    wardenDao.getEventById(eventId)?.let {
                        supabaseSyncService.syncWardenEvent(it)
                    }
                }
            }
        }
    }

    private fun stopUnplannedBreak() {
        serviceScope.launch {
            val breakEndTime = System.currentTimeMillis()
            var breakDuration = 0L

            // Finalize the unplanned break event and get its duration
            currentWardenEventId?.let {
                wardenDao.updateEventEndTime(it, breakEndTime)
                wardenDao.getEventById(it)?.let { event ->
                    breakDuration = breakEndTime - event.startTime
                    supabaseSyncService.syncWardenEvent(event)
                }
                currentWardenEventId = null
            }

            // Restore the previous state
            val previousState = stateBeforeUnplannedBreak
            _timerState.value = previousState ?: TimerState()
            currentWardenEventId = wardenEventIdBeforeUnplannedBreak
            stateBeforeUnplannedBreak = null
            wardenEventIdBeforeUnplannedBreak = null

            // Adjust quest start time if we are returning to a quest
            if (previousState?.mode == TimerMode.QUEST_COUNTDOWN && previousState.activeQuestId != null && breakDuration > 0) {
                questDao.getQuestById(previousState.activeQuestId)?.let {
                    val updatedQuest = it.copy(quest_started_at = it.quest_started_at + breakDuration)
                    questDao.upsertQuest(updatedQuest)
                }
            }

            // If we are returning to an active event, re-register it and start the updater
            if (currentWardenEventId != null) {
                startEndTimeUpdater()
            }
        }
    }

    private fun startUnplannedBreak() {
        serviceScope.launch {
            stateBeforeUnplannedBreak = _timerState.value
            wardenEventIdBeforeUnplannedBreak = currentWardenEventId

            val event = WardenEvent(
                name = "unplanned_break",
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                color_rgba = "128,128,128,255" // Gray color
            )
            handleEventInsertion(event)
            _timerState.value = TimerState(mode = TimerMode.UNPLANNED_BREAK, time = Duration.ZERO)
        }
    }

    private fun completeQuest() {
        serviceScope.launch {
            val questId = _timerState.value.activeQuestId ?: return@launch
            val quest = questDao.getQuestById(questId) ?: return@launch
            val now = System.currentTimeMillis()

            cancelAlarmsForQuest(questId)

            val completedQuest = quest.copy(
                last_completed_on = getCurrentDate(),
                last_completed_at = now
            )
            questDao.upsertQuest(completedQuest)
            temporaryAddedTimeMillis = 0L // Reset added time

            // Force an immediate state update
            updateTimerState()
        }
    }

    private fun addTime(timeInMillis: Long) {
        // Add time only if the timer is in QUEST_COUNTDOWN mode.
        if (_timerState.value.mode == TimerMode.QUEST_COUNTDOWN && timeInMillis > 0) {
            temporaryAddedTimeMillis += timeInMillis
        }
    }

    private fun toggleInfoMode() {
        infoModeActive = !infoModeActive
        if (infoModeActive) {
            stateBeforeInfoMode = _timerState.value
        } else {
            stateBeforeInfoMode = null
        }

        // Force an immediate update to reflect the change
        serviceScope.launch {
            updateTimerState()
        }
    }

    private fun handleEventInsertion(event: WardenEvent) {
        serviceScope.launch {
            // Stop the updater for the previous event
            endTimeUpdateJob?.cancel()

            // Get the start time for the new event, which will be the end time for the previous one.
            val newEventStartTime = event.startTime

            // Update the previous event with its final end time
            currentWardenEventId?.let { previousEventId ->
                wardenDao.updateEventEndTime(previousEventId, newEventStartTime)
                // Sync the final state of the previous event
                wardenDao.getEventById(previousEventId)?.let {
                    supabaseSyncService.syncWardenEvent(it)
                }
            }

            // Insert the new event and get its ID
            val newEventId = wardenDao.insertEvent(event).toInt()
            currentWardenEventId = newEventId
            val eventToSync = event.copy(id = newEventId)
            supabaseSyncService.syncWardenEvent(eventToSync)

            // Start the real-time updater for the new event
            startEndTimeUpdater()
        }
    }

    companion object {
        const val ACTION_START_UNPLANNED_BREAK = "neth.iecal.questphone.action.START_UNPLANNED_BREAK"
        const val ACTION_STOP_UNPLANNED_BREAK = "neth.iecal.questphone.action.STOP_UNPLANNED_BREAK"
        const val ACTION_COMPLETE_QUEST = "neth.iecal.questphone.action.COMPLETE_QUEST"
        const val ACTION_ADD_TIME = "neth.iecal.questphone.action.ADD_TIME"
        const val ACTION_TOGGLE_INFO_MODE = "neth.iecal.questphone.action.TOGGLE_INFO_MODE"

        const val EXTRA_TIME_TO_ADD = "neth.iecal.questphone.extra.TIME_TO_ADD"

        private const val OVERDUE_QUEST_ALARM_OFFSET = 10000 // Offset to avoid collision
        private const val OVERDUE_BREAK_ALARM_OFFSET = 20000 // Offset to avoid collision
        private val _timerState = MutableStateFlow(TimerState())
        val timerState = _timerState.asStateFlow()
    }
}
