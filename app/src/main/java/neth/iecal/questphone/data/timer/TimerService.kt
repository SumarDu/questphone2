package neth.iecal.questphone.data.timer

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.data.local.QuestEvent
import neth.iecal.questphone.data.local.QuestEventDao
import neth.iecal.questphone.data.remote.SupabaseSyncService
import neth.iecal.questphone.data.timer.AlarmHelper
import neth.iecal.questphone.utils.NotificationHelper
import neth.iecal.questphone.utils.getCurrentDate
import java.time.Duration
import java.util.concurrent.TimeUnit

class TimerService : Service() {

    private val eventLoggingMutex = Mutex()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var questDao: neth.iecal.questphone.data.quest.QuestDao
    private lateinit var questEventDao: QuestEventDao
    private lateinit var supabaseSyncService: SupabaseSyncService
    private var stateBeforeUnplannedBreak: TimerState? = null
    private var unplannedBreakStartTime: Long? = null
    private var temporaryAddedTimeMillis = 0L
    private var infoModeActive = false
    private var stateBeforeInfoMode: TimerState? = null
    private var eventUpdateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        questDao = neth.iecal.questphone.data.quest.QuestDatabaseProvider.getInstance(this).questDao()
        questEventDao = AppDatabase.getDatabase(this).questEventDao()
        supabaseSyncService = SupabaseSyncService(this)
        notificationHelper.createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
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
            ACTION_START_UNPLANNED_BREAK -> {
                startUnplannedBreak()
                return START_STICKY
            }
            ACTION_STOP_UNPLANNED_BREAK -> {
                stopUnplannedBreak()
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

        // Handle unplanned break first to prevent it from being overridden
        if (_timerState.value.mode == TimerMode.UNPLANNED_BREAK) {
            unplannedBreakStartTime?.let { startTime ->
                val duration = Duration.ofMillis(now - startTime)
                _timerState.value = _timerState.value.copy(time = duration)
            }
            updateNotification()
            return
        }

        val activeQuest = allQuests.firstOrNull { it.quest_started_at > 0 && it.last_completed_on != today }
        val recentlyCompleted = allQuests
            .filter { it.last_completed_at > it.quest_started_at && it.break_duration_minutes > 0 }
            .maxByOrNull { it.last_completed_at }

        var currentStateHandled = false

        if (infoModeActive) {
            val referenceState = stateBeforeInfoMode ?: return
            val questId = referenceState.activeQuestId ?: return

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
                            startTime = null
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
                else -> startTime = null
            }

            if (stillValid && startTime != null) {
                val elapsed = Duration.ofMillis(now - startTime)
                _timerState.value = referenceState.copy(mode = TimerMode.INFO, time = elapsed)
            } else {
                infoModeActive = false
                stateBeforeInfoMode = null
            }
            updateNotification()
            return
        }

        if (activeQuest == null && temporaryAddedTimeMillis != 0L) {
            temporaryAddedTimeMillis = 0L
        }

        if (previousState.activeQuestId != null && previousState.activeQuestId != activeQuest?.id) {
            cancelAllAlarmsForQuest(previousState.activeQuestId)
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
                    // Set quest completion alarm
                    AlarmHelper.setAlarm(this, actualQuestEndsAt, activeQuest.id.hashCode(), AlarmHelper.ALARM_TYPE_QUEST_COMPLETE, activeQuest.id, "Quest Complete!", "'${activeQuest.title}' is complete!")
                    
                    // Set 1-minute warning alarm if quest has more than 1 minute remaining
                    val oneMinuteWarningTime = actualQuestEndsAt - TimeUnit.MINUTES.toMillis(1)
                    if (now < oneMinuteWarningTime) {
                        AlarmHelper.setAlarm(this, oneMinuteWarningTime, activeQuest.id.hashCode() + 100, AlarmHelper.ALARM_TYPE_QUEST_WARNING, activeQuest.id, "Quest Ending Soon", "'${activeQuest.title}' will end in 1 minute!")
                    }
                }
            } else {
                val wasNotOvertime = previousState.mode != TimerMode.OVERTIME || previousState.isBreakOvertime
                _timerState.value = TimerState(
                    mode = TimerMode.OVERTIME,
                    time = Duration.ofMillis(-remaining),
                    activeQuestId = activeQuest.id,
                    isBreakOvertime = false,
                    questEndsAt = actualQuestEndsAt
                )
                // Set up overdue notifications when first entering quest overdue state
                if (wasNotOvertime) {
                    cancelAlarmsForQuest(activeQuest.id)
                    AlarmHelper.setAlarm(this, now + OVERDUE_QUEST_ALARM_OFFSET, activeQuest.id.hashCode() + OVERDUE_QUEST_ALARM_OFFSET, AlarmHelper.ALARM_TYPE_QUEST_OVERDUE, activeQuest.id, "Quest Overdue!", "You are running overtime on '${activeQuest.title}'!")
                    
                    // Start periodic overdue notifications (every 3 minutes)
                    val firstPeriodicNotification = now + TimeUnit.MINUTES.toMillis(3)
                    android.util.Log.d("TimerService", "Setting up periodic overdue notification for quest ${activeQuest.id} at time $firstPeriodicNotification (in ${TimeUnit.MINUTES.toMillis(3)/1000} seconds)")
                    AlarmHelper.setAlarm(this, firstPeriodicNotification, activeQuest.id.hashCode() + 1000, AlarmHelper.ALARM_TYPE_OVERDUE_PERIODIC, activeQuest.id, "Quest Still Overdue", "You are still overdue on '${activeQuest.title}'. Please complete it.")
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
                } catch (e: Exception) { /* Log error */ }
            }

            if (now < breakEndsAt) {
                val timeUntilBreakEnd = Duration.ofMillis(breakEndsAt - now)
                _timerState.value = TimerState(TimerMode.BREAK, timeUntilBreakEnd, recentlyCompleted.id, isDeepFocusLocking = isLocking)
                if (previousState.mode != TimerMode.BREAK) {
                    cancelAlarmsForQuest(recentlyCompleted.id)
                    // Set break end alarm
                    AlarmHelper.setAlarm(this, breakEndsAt, recentlyCompleted.id.hashCode(), AlarmHelper.ALARM_TYPE_BREAK_OVER, recentlyCompleted.id, "Break Over!", "Time to get back to it.")
                    
                    // Set 1-minute warning alarm if break has more than 1 minute remaining
                    val oneMinuteWarningTime = breakEndsAt - TimeUnit.MINUTES.toMillis(1)
                    if (now < oneMinuteWarningTime) {
                        AlarmHelper.setAlarm(this, oneMinuteWarningTime, recentlyCompleted.id.hashCode() + 200, AlarmHelper.ALARM_TYPE_BREAK_WARNING, recentlyCompleted.id, "Break Ending Soon", "Your break will end in 1 minute!")
                    }
                }
            } else {
                val overtimeDuration = Duration.ofMillis(now - breakEndsAt)
                _timerState.value = TimerState(TimerMode.OVERTIME, overtimeDuration, recentlyCompleted.id, isBreakOvertime = true, isDeepFocusLocking = isLocking)
                if (previousState.mode != TimerMode.OVERTIME || !previousState.isBreakOvertime) {
                    cancelAlarmsForQuest(recentlyCompleted.id)
                    AlarmHelper.setAlarm(this, now + OVERDUE_BREAK_ALARM_OFFSET, recentlyCompleted.id.hashCode() + OVERDUE_BREAK_ALARM_OFFSET, AlarmHelper.ALARM_TYPE_BREAK_OVERDUE, recentlyCompleted.id, "Break Overdue!", "You're taking too long of a break.")
                    
                    // Start periodic overdue notifications (every 3 minutes)
                    val firstPeriodicNotification = now + TimeUnit.MINUTES.toMillis(3)
                    AlarmHelper.setAlarm(this, firstPeriodicNotification, recentlyCompleted.id.hashCode() + 1000, AlarmHelper.ALARM_TYPE_OVERDUE_PERIODIC, recentlyCompleted.id, "Break Still Overdue", "You are still taking too long of a break. Please return to work.")
                }
            }
        }



        if (!currentStateHandled && _timerState.value.mode != TimerMode.INACTIVE) {
            infoModeActive = false
            _timerState.value = TimerState(TimerMode.INACTIVE)
        }

        handleEventLogging(previousState, _timerState.value)
        updateNotification()
    }

    private fun cancelAlarmsForQuest(questId: String) {
        val questIdHash = questId.hashCode()
        // Cancel basic alarms
        AlarmHelper.cancelAlarm(this, questIdHash)
        AlarmHelper.cancelAlarm(this, questIdHash + OVERDUE_QUEST_ALARM_OFFSET)
        AlarmHelper.cancelAlarm(this, questIdHash + OVERDUE_BREAK_ALARM_OFFSET)
        
        // Cancel warning alarms (1-minute before end)
        AlarmHelper.cancelAlarm(this, questIdHash + 100) // Quest warning
        AlarmHelper.cancelAlarm(this, questIdHash + 200) // Break warning
    }
    
    private fun cancelAllAlarmsForQuest(questId: String) {
        val questIdHash = questId.hashCode()
        // Cancel basic alarms
        AlarmHelper.cancelAlarm(this, questIdHash)
        AlarmHelper.cancelAlarm(this, questIdHash + OVERDUE_QUEST_ALARM_OFFSET)
        AlarmHelper.cancelAlarm(this, questIdHash + OVERDUE_BREAK_ALARM_OFFSET)
        
        // Cancel warning alarms (1-minute before end)
        AlarmHelper.cancelAlarm(this, questIdHash + 100) // Quest warning
        AlarmHelper.cancelAlarm(this, questIdHash + 200) // Break warning
        
        // Cancel periodic notification alarms
        AlarmHelper.cancelAlarm(this, questIdHash + 1000) // Overdue periodic
        AlarmHelper.cancelAlarm(this, questIdHash + 2000) // Unplanned break periodic
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
            String.format("%02d:%02d:%02d", absSeconds / 3600, (absSeconds % 3600) / 60, absSeconds % 60)
        } else {
            String.format("%02d:%02d", (absSeconds % 3600) / 60, absSeconds % 60)
        }
        return if (seconds < 0) "-$positive" else positive
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Cancel the scope to clean up coroutines
    }

    private fun stopUnplannedBreak() {
        serviceScope.launch {
            val breakEndTime = System.currentTimeMillis()
            var breakDuration = 0L
            unplannedBreakStartTime?.let { breakDuration = breakEndTime - it }
            unplannedBreakStartTime = null

            val previousState = stateBeforeUnplannedBreak
            _timerState.value = previousState ?: TimerState()
            
            // Cancel periodic unplanned break notifications
            val activeQuestId = previousState?.activeQuestId ?: "unplanned_break"
            AlarmHelper.cancelAlarm(this@TimerService, activeQuestId.hashCode() + 2000)
            
            stateBeforeUnplannedBreak = null

            if (previousState?.mode == TimerMode.QUEST_COUNTDOWN && previousState.activeQuestId != null && breakDuration > 0) {
                questDao.getQuestById(previousState.activeQuestId)?.let { quest ->
                    val updatedQuest = quest.copy(quest_started_at = quest.quest_started_at + breakDuration)
                    questDao.upsertQuest(updatedQuest)
                    
                    // Reschedule quest notifications with adjusted timing after unplanned break
                    val now = System.currentTimeMillis()
                    val questDurationMillis = TimeUnit.MINUTES.toMillis(quest.quest_duration_minutes.toLong())
                    val adjustedQuestEndsAt = updatedQuest.quest_started_at + questDurationMillis + temporaryAddedTimeMillis
                    
                    if (now < adjustedQuestEndsAt) {
                        // Reschedule quest completion alarm
                        AlarmHelper.setAlarm(this@TimerService, adjustedQuestEndsAt, quest.id.hashCode(), AlarmHelper.ALARM_TYPE_QUEST_COMPLETE, quest.id, "Quest Complete!", "'${quest.title}' is complete!")
                        
                        // Reschedule 1-minute warning if there's still more than 1 minute remaining
                        val oneMinuteWarningTime = adjustedQuestEndsAt - TimeUnit.MINUTES.toMillis(1)
                        if (now < oneMinuteWarningTime) {
                            AlarmHelper.setAlarm(this@TimerService, oneMinuteWarningTime, quest.id.hashCode() + 100, AlarmHelper.ALARM_TYPE_QUEST_WARNING, quest.id, "Quest Ending Soon", "'${quest.title}' will end in 1 minute!")
                        }
                    }
                }
            }
        }
    }

    private fun startUnplannedBreak() {
        serviceScope.launch {
            stateBeforeUnplannedBreak = _timerState.value
            unplannedBreakStartTime = System.currentTimeMillis()
            
            // Cancel quest-related notifications (completion and 1-min warning) during unplanned break
            stateBeforeUnplannedBreak?.activeQuestId?.let { questId ->
                val questIdHash = questId.hashCode()
                AlarmHelper.cancelAlarm(this@TimerService, questIdHash) // Quest completion
                AlarmHelper.cancelAlarm(this@TimerService, questIdHash + 100) // Quest 1-min warning
            }
            
            _timerState.value = TimerState(mode = TimerMode.UNPLANNED_BREAK)
            
            // Set up periodic notifications for unplanned break (every 5 minutes)
            val activeQuestId = stateBeforeUnplannedBreak?.activeQuestId ?: "unplanned_break"
            val firstPeriodicNotification = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)
            AlarmHelper.setAlarm(
                this@TimerService, 
                firstPeriodicNotification, 
                activeQuestId.hashCode() + 2000, 
                AlarmHelper.ALARM_TYPE_UNPLANNED_BREAK_PERIODIC, 
                activeQuestId, 
                "Unplanned Break Reminder", 
                "You've been on an unplanned break for 5 minutes. Consider returning to your task."
            )
        }
    }

    private fun completeQuest() {
        serviceScope.launch {
            val questId = _timerState.value.activeQuestId ?: return@launch
            val quest = questDao.getQuestById(questId) ?: return@launch
            val now = System.currentTimeMillis()
            cancelAllAlarmsForQuest(questId)
            val completedQuest = quest.copy(last_completed_on = getCurrentDate(), last_completed_at = now)
            questDao.upsertQuest(completedQuest)
            temporaryAddedTimeMillis = 0L
            updateTimerState()
        }
    }

    private fun addTime(timeInMillis: Long) {
        if (_timerState.value.mode == TimerMode.QUEST_COUNTDOWN && timeInMillis > 0) {
            temporaryAddedTimeMillis += timeInMillis
        }
    }

    private fun getEventName(state: TimerState): String? {
        val quest = state.activeQuestId?.let { runBlocking { questDao.getQuestById(it) } }
        return when (state.mode) {
            TimerMode.QUEST_COUNTDOWN -> quest?.title
            TimerMode.OVERTIME -> if (state.isBreakOvertime) "overdue (break)" else "overdue (${quest?.title})"
            TimerMode.BREAK -> "break"
            else -> null
        }
    }

    private suspend fun handleEventLogging(previousState: TimerState, currentState: TimerState) {
        eventLoggingMutex.withLock {
            val now = System.currentTimeMillis()
            val latestEvent = questEventDao.getLatestEvent()
            val newEventName = getEventName(currentState)

            val isNewEventStarting = newEventName != null
            val isActiveEventRunning = latestEvent != null && latestEvent.endTime == 0L

            if (isNewEventStarting) {
                // If no active event is running, or the new event is different, start a new event sequence.
                if (!isActiveEventRunning || latestEvent?.eventName != newEventName) {
                    // Close the previous event if it exists and is active.
                    if (isActiveEventRunning) {
                        val closedEvent = latestEvent!!.copy(endTime = now)
                        questEventDao.updateEvent(closedEvent)
                        serviceScope.launch { supabaseSyncService.syncSingleQuestEvent(closedEvent) }
                    }

                    // Create the new event.
                    val quest = currentState.activeQuestId?.let { questDao.getQuestById(it) }
                    val colorRgba = when {
                        newEventName == "break" -> "0.13,0.59,0.2,1.0" // Green
                        newEventName == "overdue (break)" -> "0.76,0.18,0.13,1.0" // Red
                        newEventName.startsWith("overdue") -> "0.86,0.08,0.24,1.0" // Crimson
                        else -> quest?.color_rgba ?: ""
                    }

                    val newEvent = QuestEvent(
                        eventName = newEventName!!,
                        startTime = now,
                        endTime = 0L, // Mark as active
                        colorRgba = colorRgba
                    )
                    questEventDao.insertEvent(newEvent)
                    // After inserting, fetch the latest event to get the auto-generated ID
                    val eventToSync = questEventDao.getLatestEvent()
                    if (eventToSync != null) {
                        serviceScope.launch { supabaseSyncService.syncSingleQuestEvent(eventToSync) }
                    }
                }
                // Otherwise, the correct event is already running. Do nothing.
            } else { // No new event should be running
                // If an event is currently active, close it.
                if (isActiveEventRunning) {
                    val closedEvent = latestEvent!!.copy(endTime = now)
                    questEventDao.updateEvent(closedEvent)
                    serviceScope.launch { supabaseSyncService.syncSingleQuestEvent(closedEvent) }
                }
            }
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



    companion object {
        private val _timerState = MutableStateFlow(TimerState())
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
        const val ACTION_START_UNPLANNED_BREAK = "neth.iecal.questphone.action.START_UNPLANNED_BREAK"
        const val ACTION_STOP_UNPLANNED_BREAK = "neth.iecal.questphone.action.STOP_UNPLANNED_BREAK"
        const val ACTION_COMPLETE_QUEST = "neth.iecal.questphone.action.COMPLETE_QUEST"
        const val ACTION_ADD_TIME = "neth.iecal.questphone.action.ADD_TIME"
        const val ACTION_TOGGLE_INFO_MODE = "neth.iecal.questphone.action.TOGGLE_INFO_MODE"

        const val EXTRA_TIME_TO_ADD = "neth.iecal.questphone.extra.TIME_TO_ADD"

        private const val OVERDUE_QUEST_ALARM_OFFSET = 10000 // Offset to avoid collision
        private const val OVERDUE_BREAK_ALARM_OFFSET = 20000 // Offset to avoid collision
    }
}
