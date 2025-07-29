package neth.iecal.questphone.data.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
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
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.VibrationHelper
import neth.iecal.questphone.utils.SchedulingUtils
import java.time.Duration
import java.util.concurrent.TimeUnit

class TimerService : Service() {

    private val eventLoggingMutex = Mutex()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        questDao = neth.iecal.questphone.data.quest.QuestDatabaseProvider.getInstance(this).questDao()
        questEventDao = AppDatabase.getDatabase(this).questEventDao()
        supabaseSyncService = SupabaseSyncService(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Notifications"
            val descriptionText = "Notifications for quest and break timers"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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

        // Timer service now runs without notifications
        // startForeground removed - service will run in background

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
                if (now - _timerState.value.lastOverdueNotificationTime >= TimeUnit.MINUTES.toMillis(5)) {
                    sendNotification("Unplanned Break", "Still on an unplanned break.", NotificationType.UNPLANNED_BREAK)
                    _timerState.value = _timerState.value.copy(time = duration, lastOverdueNotificationTime = now)
                } else {
                    _timerState.value = _timerState.value.copy(time = duration)
                }
            }
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
    
            return
        }

        if (activeQuest == null && temporaryAddedTimeMillis != 0L) {
            temporaryAddedTimeMillis = 0L
        }

        if (previousState.activeQuestId != null && previousState.activeQuestId != activeQuest?.id) {
            // Removed alarm cancellation
        }

        if (activeQuest != null && activeQuest.quest_duration_minutes > 0) {
            currentStateHandled = true
            val questDurationMillis = TimeUnit.MINUTES.toMillis(activeQuest.quest_duration_minutes.toLong())
            val actualQuestEndsAt = activeQuest.quest_started_at + questDurationMillis + temporaryAddedTimeMillis
            val remaining = actualQuestEndsAt - now
            val needsAlarmUpdate = previousState.questEndsAt != actualQuestEndsAt

            if (now < actualQuestEndsAt) {
                val remainingSeconds = remaining / 1000
                if (remainingSeconds <= 60 && !previousState.notificationSent) {
                    sendNotification("Quest ending soon!", "1 minute remaining.", NotificationType.QUEST_ENDING)
                    _timerState.value = _timerState.value.copy(notificationSent = true)
                }

                _timerState.value = TimerState(
                    mode = TimerMode.QUEST_COUNTDOWN,
                    time = Duration.ofMillis(remaining),
                    activeQuestId = activeQuest.id,
                    questEndsAt = actualQuestEndsAt,
                    notificationSent = _timerState.value.notificationSent
                )
            } else {
                val wasNotOvertime = previousState.mode != TimerMode.OVERTIME || previousState.isBreakOvertime
                if (now - previousState.lastOverdueNotificationTime >= TimeUnit.MINUTES.toMillis(3)) {
                    sendNotification("Quest Overdue!", "The quest is now overdue.", NotificationType.QUEST_OVERDUE)
                    _timerState.value = TimerState(
                        mode = TimerMode.OVERTIME,
                        time = Duration.ofMillis(-remaining),
                        activeQuestId = activeQuest.id,
                        isBreakOvertime = false,
                        questEndsAt = actualQuestEndsAt,
                        lastOverdueNotificationTime = now
                    )
                } else {
                    _timerState.value = TimerState(
                        mode = TimerMode.OVERTIME,
                        time = Duration.ofMillis(-remaining),
                        activeQuestId = activeQuest.id,
                        isBreakOvertime = false,
                        questEndsAt = actualQuestEndsAt,
                        lastOverdueNotificationTime = previousState.lastOverdueNotificationTime
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
                } catch (e: Exception) { /* Log error */ }
            }

            if (now < breakEndsAt) {
                val timeUntilBreakEnd = Duration.ofMillis(breakEndsAt - now)
                val remainingSeconds = timeUntilBreakEnd.seconds

                if (remainingSeconds <= 60 && !previousState.notificationSent) {
                    sendNotification("Break ending soon!", "1 minute remaining.", NotificationType.BREAK_ENDING)
                    _timerState.value = _timerState.value.copy(notificationSent = true)
                }

                _timerState.value = TimerState(TimerMode.BREAK, timeUntilBreakEnd, recentlyCompleted.id, isDeepFocusLocking = isLocking, notificationSent = _timerState.value.notificationSent)
            } else {
                val overtimeDuration = Duration.ofMillis(now - breakEndsAt)
                if (now - previousState.lastOverdueNotificationTime >= TimeUnit.MINUTES.toMillis(3)) {
                    sendNotification("Break Overdue!", "The break is now overdue.", NotificationType.BREAK_OVERDUE)
                    _timerState.value = TimerState(TimerMode.OVERTIME, overtimeDuration, recentlyCompleted.id, isBreakOvertime = true, isDeepFocusLocking = isLocking, lastOverdueNotificationTime = now)
                } else {
                    _timerState.value = TimerState(TimerMode.OVERTIME, overtimeDuration, recentlyCompleted.id, isBreakOvertime = true, isDeepFocusLocking = isLocking, lastOverdueNotificationTime = previousState.lastOverdueNotificationTime)
                }
            }
        }



        if (!currentStateHandled && _timerState.value.mode != TimerMode.INACTIVE) {
            infoModeActive = false
            _timerState.value = TimerState(TimerMode.INACTIVE, notificationSent = false)
        }

        handleEventLogging(previousState, _timerState.value)

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
            
            stateBeforeUnplannedBreak = null

            if (previousState?.mode == TimerMode.QUEST_COUNTDOWN && previousState.activeQuestId != null && breakDuration > 0) {
                questDao.getQuestById(previousState.activeQuestId)?.let { quest ->
                    val updatedQuest = quest.copy(quest_started_at = quest.quest_started_at + breakDuration)
                    questDao.upsertQuest(updatedQuest)
                    // Quest rescheduling notifications removed
                }
            }
        }
    }

    private fun startUnplannedBreak() {
        serviceScope.launch {
            stateBeforeUnplannedBreak = _timerState.value
            unplannedBreakStartTime = System.currentTimeMillis()
            
            _timerState.value = TimerState(mode = TimerMode.UNPLANNED_BREAK)
            // Unplanned break notifications removed
        }
    }

    private fun completeQuest() {
        serviceScope.launch {
            val questId = _timerState.value.activeQuestId ?: return@launch
            val quest = questDao.getQuestById(questId) ?: return@launch
            val now = System.currentTimeMillis()
            
            // Update scheduling info for next occurrence
            val updatedSchedulingInfo = SchedulingUtils.updateSchedulingForNextOccurrence(quest.scheduling_info)
            
            val completedQuest = if (updatedSchedulingInfo == null) {
                // For specific date quests, mark as destroyed and set proper expiration
                quest.copy(
                    last_completed_on = getCurrentDate(),
                    last_completed_at = now,
                    is_destroyed = true,
                    auto_destruct = SchedulingUtils.getExpirationDate(quest.scheduling_info)
                )
            } else {
                // For repeating quests, update scheduling info
                quest.copy(
                    last_completed_on = getCurrentDate(),
                    last_completed_at = now,
                    scheduling_info = updatedSchedulingInfo
                )
            }
            
            questDao.upsertQuest(completedQuest)
            temporaryAddedTimeMillis = 0L
            _timerState.value = _timerState.value.copy(notificationSent = false) // Reset notification flag
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

    private enum class NotificationType {
        QUEST_ENDING,
        BREAK_ENDING,
        QUEST_OVERDUE,
        BREAK_OVERDUE,
        UNPLANNED_BREAK
    }

    private fun sendNotification(title: String, message: String, type: NotificationType) {
        val soundUri = getSoundForNotificationType(type)
        val vibrationPattern = getVibrationPatternForNotificationType(type)
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(neth.iecal.questphone.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(vibrationPattern)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(type.ordinal + 1, notification)
        
        // Trigger vibration manually for better control
        triggerVibration(type)
    }
    
    private fun getSoundForNotificationType(type: NotificationType): Uri {
        val soundResource = when (type) {
            NotificationType.QUEST_ENDING -> neth.iecal.questphone.R.raw.quest_expired
            NotificationType.BREAK_ENDING -> neth.iecal.questphone.R.raw.break_expiried
            NotificationType.QUEST_OVERDUE -> neth.iecal.questphone.R.raw.overdue_quest
            NotificationType.BREAK_OVERDUE -> neth.iecal.questphone.R.raw.overdue_break
            NotificationType.UNPLANNED_BREAK -> neth.iecal.questphone.R.raw.overdue_break
        }
        return Uri.parse("android.resource://" + packageName + "/" + soundResource)
    }
    
    private fun getVibrationPatternForNotificationType(type: NotificationType): LongArray {
        return when (type) {
            NotificationType.QUEST_ENDING -> longArrayOf(0, 300, 100, 300) // Short-pause-short
            NotificationType.BREAK_ENDING -> longArrayOf(0, 500, 200, 500) // Medium-pause-medium
            NotificationType.QUEST_OVERDUE -> longArrayOf(0, 200, 100, 200, 100, 200) // Triple short
            NotificationType.BREAK_OVERDUE -> longArrayOf(0, 400, 200, 400, 200, 400) // Triple medium
            NotificationType.UNPLANNED_BREAK -> longArrayOf(0, 1000, 300, 1000) // Long-pause-long
        }
    }
    
    private fun triggerVibration(type: NotificationType) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        
        val pattern = getVibrationPatternForNotificationType(type)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
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

        private const val CHANNEL_ID = "timer_channel"
        private const val VIBRATOR_MANAGER_SERVICE = "vibrator_manager"
    }
}
