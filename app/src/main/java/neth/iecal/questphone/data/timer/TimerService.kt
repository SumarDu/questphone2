package neth.iecal.questphone.data.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
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
import neth.iecal.questphone.data.SchedulingType
import neth.iecal.questphone.data.timer.TimerState
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.data.local.QuestEvent
import neth.iecal.questphone.data.local.QuestEventDao
import neth.iecal.questphone.data.remote.SupabaseSyncService
import neth.iecal.questphone.services.INTENT_ACTION_UNLOCK_APP
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
    private var unplannedBreakEventId: String? = null
    private var temporaryAddedTimeMillis = 0L
    private var infoModeActive = false
    private var stateBeforeInfoMode: TimerState? = null
    private var eventUpdateJob: Job? = null
    private var breakEndedEarly = false
    private var breakTerminationTime: Long? = null
    private var unlockEndTime: Long? = null
    private var currentUnlockDurationMillis: Long? = null
    private var preUnlockTimerState: TimerState? = null

    override fun onCreate() {
        super.onCreate()
        questDao = neth.iecal.questphone.data.quest.QuestDatabaseProvider.getInstance(this).questDao()
        questEventDao = AppDatabase.getDatabase(this).questEventDao()
        supabaseSyncService = SupabaseSyncService(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // Channel for regular, time-sensitive alerts
            val timerChannel = NotificationChannel("timer_channel", "Timer Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for quest/break ending, etc."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(timerChannel)

            // Channel for overdue alerts
            val overdueChannel = NotificationChannel("overdue_channel", "Overdue Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for when quests or breaks go overdue."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(overdueChannel)

            // Channel for the silent, ongoing unlock progress
            val unlockChannel = NotificationChannel("unlock_channel", "Unlock Progress", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the remaining time for an app unlock."
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(unlockChannel)
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
                val reason = intent.getStringExtra("UNPLANNED_BREAK_REASON") ?: ""
                startUnplannedBreak(reason)
                return START_STICKY
            }
            ACTION_STOP_UNPLANNED_BREAK -> {
                stopUnplannedBreak()
                return START_STICKY
            }
            ACTION_END_BREAK_EARLY -> {
                Log.d("TimerService", "Received ACTION_END_BREAK_EARLY.")
                val triggeredByUnlock = intent?.getBooleanExtra("triggered_by_unlock", false) ?: false
                if (triggeredByUnlock) {
                    val unlockDuration = intent?.getIntExtra("unlock_duration_minutes", 0) ?: 0
                    val packageName = intent?.getStringExtra("package_name") ?: ""
                    endBreakEarly(unlockDuration, packageName)
                } else {
                    endBreakEarly()
                }
                return START_STICKY
            }
            ACTION_START_UNLOCK_TIMER -> {
                val unlockDuration = intent.getIntExtra(EXTRA_UNLOCK_DURATION, 0)
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                val rewardCoins = intent.getIntExtra(EXTRA_REWARD_COINS, 0)
                val preRewardCoins = intent.getIntExtra(EXTRA_PRE_REWARD_COINS, 0)

                if (unlockDuration > 0 && packageName != null) {
                    startUnlockTimer(unlockDuration, packageName, rewardCoins, preRewardCoins)
                }
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
        
        // Handle unlock timer
        if (_timerState.value.mode == TimerMode.UNLOCK) {
            unlockEndTime?.let { endTime ->
                if (now < endTime) {
                    val remainingMillis = endTime - now
                    val totalDurationMillis = currentUnlockDurationMillis ?: (1000 * 60) // Fallback to 1 min

                    // Update the persistent progress notification
                    val appName = _timerState.value.unlockPackageName?.let { getAppNameFromPackage(it) } ?: "App"
                    sendNotification(
                        title = "$appName Unlocked",
                        message = formatDuration(Duration.ofMillis(remainingMillis)) + " remaining",
                        type = NotificationType.UNLOCK_PROGRESS,
                        progress = (totalDurationMillis - remainingMillis).toInt(),
                        maxProgress = totalDurationMillis.toInt()
                    )

                    // Handle the 1-minute warning notification
                    if (remainingMillis <= 60000 && !_timerState.value.unlockNotificationSent) {
                        sendNotification("Unlock Ending Soon", "1 minute remaining.", NotificationType.UNLOCK_ENDING)
                        _timerState.value = _timerState.value.copy(
                            time = Duration.ofMillis(remainingMillis),
                            unlockNotificationSent = true
                        )
                    } else {
                        _timerState.value = _timerState.value.copy(time = Duration.ofMillis(remainingMillis))
                    }
                } else {
                    // Unlock timer expired, dismiss the progress notification and restore the previous state
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(NotificationType.UNLOCK_PROGRESS.ordinal + 1)

                    currentUnlockDurationMillis?.let { duration ->
                        breakTerminationTime?.let { terminationTime ->
                            breakTerminationTime = terminationTime + duration
                            Log.d("TimerService", "Adjusted breakTerminationTime by $duration ms.")
                        }
                    }

                    preUnlockTimerState?.let {
                        _timerState.value = it
                        Log.d("TimerService", "Unlock finished. Restored state to: ${it.mode}")
                    } ?: run {
                        // Fallback to INACTIVE if there's no saved state
                        _timerState.value = _timerState.value.copy(
                            mode = TimerMode.INACTIVE,
                            time = Duration.ZERO
                        )
                        Log.w("TimerService", "Unlock finished, but no pre-unlock state was saved. Setting to INACTIVE.")
                    }
                    unlockEndTime = null
                    preUnlockTimerState = null // Clear the saved state
                    currentUnlockDurationMillis = null // Clear the duration
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

        // Handle overdue state from an early break termination
        if (_timerState.value.mode == TimerMode.OVERTIME && _timerState.value.isBreakOvertime && breakEndedEarly) {
            breakTerminationTime?.let { terminationTime ->
                val overtimeDuration = Duration.ofMillis(now - terminationTime)
                if (now - _timerState.value.lastOverdueNotificationTime >= TimeUnit.MINUTES.toMillis(3)) {
                    sendNotification("Break Overdue!", "The break is now overdue.", NotificationType.BREAK_OVERDUE)
                    _timerState.value = _timerState.value.copy(time = overtimeDuration, lastOverdueNotificationTime = now)
                } else {
                    _timerState.value = _timerState.value.copy(time = overtimeDuration)
                }
                currentStateHandled = true
            }
        }

        if (previousState.activeQuestId != null && previousState.activeQuestId != _timerState.value.activeQuestId) {
            handleEventLogging(previousState, _timerState.value)
        }

        if (activeQuest != null && activeQuest.quest_duration_minutes > 0) {
            // Reset break flags if a quest is active to ensure a clean state
            if (breakEndedEarly || breakTerminationTime != null) {
                Log.d("TimerService", "New quest started, resetting break flags.")
                breakEndedEarly = false
                breakTerminationTime = null
            }
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

            // Check if break was ended early - if so, skip normal break logic
            if (breakEndedEarly) {
                Log.d("TimerService", "Break ended early flag is set, maintaining OVERTIME state")
                val terminatedAt = breakTerminationTime
                if (terminatedAt != null) {
                    val overtimeDuration = Duration.ofMillis(now - terminatedAt)
                    _timerState.value = TimerState(TimerMode.OVERTIME, overtimeDuration, recentlyCompleted.id, isBreakOvertime = true, isDeepFocusLocking = isLocking, lastOverdueNotificationTime = _timerState.value.lastOverdueNotificationTime)
                } else {
                    // Fallback in case timestamp is missing
                    val overtimeDuration = Duration.ofMillis(now - breakEndsAt)
                    _timerState.value = TimerState(TimerMode.OVERTIME, overtimeDuration, recentlyCompleted.id, isBreakOvertime = true, isDeepFocusLocking = isLocking, lastOverdueNotificationTime = _timerState.value.lastOverdueNotificationTime)
                }
            } else if (now < breakEndsAt) {
                val timeUntilBreakEnd = Duration.ofMillis(breakEndsAt - now)
                val remainingSeconds = timeUntilBreakEnd.seconds

                if (remainingSeconds <= 60 && !previousState.notificationSent) {
                    sendNotification("Break ending soon!", "1 minute remaining.", NotificationType.BREAK_ENDING)
                    _timerState.value = _timerState.value.copy(notificationSent = true)
                }

                _timerState.value = TimerState(TimerMode.BREAK, timeUntilBreakEnd, recentlyCompleted.id, isDeepFocusLocking = isLocking, notificationSent = _timerState.value.notificationSent)
            } else {
                // Natural break end - reset the early termination flag
                breakEndedEarly = false
                breakTerminationTime = null
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

        // Skip event logging during unplanned breaks to prevent automatic event creation
        // Unplanned break events are handled manually in startUnplannedBreak/stopUnplannedBreak
        // ВАЖЛИВО: Також перевіряємо, чи не активна незапланована перерва через змінні стану
        val isUnplannedBreakActive = unplannedBreakStartTime != null || unplannedBreakEventId != null
        if (_timerState.value.mode != TimerMode.UNPLANNED_BREAK && 
            previousState.mode != TimerMode.UNPLANNED_BREAK && 
            !isUnplannedBreakActive) {
            handleEventLogging(previousState, _timerState.value)
        }

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
            unplannedBreakStartTime?.let { startTime ->
                breakDuration = breakEndTime - startTime
            }
            
            var unplannedBreakEndTime: Long? = null
            
            // Update the unplanned break event with end time
            unplannedBreakEventId?.let { eventId ->
                val existingEvent = questEventDao.getEventById(eventId)
                existingEvent?.let { event ->
                    val updatedEvent = event.copy(endTime = breakEndTime)
                    questEventDao.updateEvent(updatedEvent)
                    unplannedBreakEndTime = breakEndTime
                    
                    // Sync updated event to Supabase
                    supabaseSyncService.syncSingleQuestEvent(updatedEvent)
                }
            }
            
            // Reset unplanned break tracking variables
            unplannedBreakStartTime = null
            unplannedBreakEventId = null

            val previousState = stateBeforeUnplannedBreak
            _timerState.value = previousState ?: TimerState()
            
            stateBeforeUnplannedBreak = null

            // Create a resumed quest event if there was an active quest before the break
            // ВАЖЛИВО: startTime відновленої події = endTime незапланованої перерви
            
            // Check if the latest event was a checkpoint created recently (within 3 seconds)
            // This prevents race condition between createCheckpoint and stopUnplannedBreak
            val latestEvent = questEventDao.getLatestEvent()
            val now = System.currentTimeMillis()
            val isRecentCheckpoint = latestEvent?.eventName?.startsWith("cp:") == true && 
                now - latestEvent.startTime < 3000
            
            if (!isRecentCheckpoint && previousState?.mode == TimerMode.QUEST_COUNTDOWN && previousState.activeQuestId != null && unplannedBreakEndTime != null) {
                questDao.getQuestById(previousState.activeQuestId)?.let { quest ->
                    // Update quest timing to account for the break duration
                    if (breakDuration > 0) {
                        val updatedQuest = quest.copy(quest_started_at = quest.quest_started_at + breakDuration)
                        questDao.upsertQuest(updatedQuest)
                    }
                    
                    // Create a new event for the resumed quest
                    // StartTime дорівнює endTime незапланованої перерви
                    val resumedQuestEvent = neth.iecal.questphone.data.local.QuestEvent(
                        eventName = quest.title,
                        startTime = unplannedBreakEndTime!!, // Використовуємо endTime перерви
                        endTime = 0L // Active event
                    )
                    
                    questEventDao.insertEvent(resumedQuestEvent)
                    supabaseSyncService.syncSingleQuestEvent(resumedQuestEvent)
                }
            }
        }
    }

    private fun startUnplannedBreak(reason: String) {
        serviceScope.launch {
            stateBeforeUnplannedBreak = _timerState.value
            val breakStartTime = System.currentTimeMillis()
            unplannedBreakStartTime = breakStartTime

            // Immediately close the currently active quest event
            val latestEvent = questEventDao.getLatestEvent()
            if (latestEvent != null && latestEvent.endTime == 0L) {
                val closedEvent = latestEvent.copy(endTime = breakStartTime)
                questEventDao.updateEvent(closedEvent)
                supabaseSyncService.syncSingleQuestEvent(closedEvent)
            }

            // Create and log the new "unplanned break" event
            val unplannedBreakEvent = neth.iecal.questphone.data.local.QuestEvent(
                eventName = "unplanned break",
                startTime = breakStartTime,
                endTime = 0L, // This will be set when the break ends
                comments = reason
            )

            questEventDao.insertEvent(unplannedBreakEvent)
            unplannedBreakEventId = unplannedBreakEvent.id

            supabaseSyncService.syncSingleQuestEvent(unplannedBreakEvent)

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

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.w("TimerService", "Could not get app name for package: $packageName", e)
            packageName // Fallback to package name if app name can't be retrieved
        }
    }

    private fun getEventName(state: TimerState): String? {
        val quest = state.activeQuestId?.let { runBlocking { questDao.getQuestById(it) } }
        return when (state.mode) {
            TimerMode.QUEST_COUNTDOWN -> quest?.title
            TimerMode.OVERTIME -> if (state.isBreakOvertime) "overdue (break)" else "overdue (${quest?.title})"
            TimerMode.BREAK -> "break"
            TimerMode.UNLOCK -> {
                state.unlockPackageName?.let { packageName ->
                    val appName = getAppNameFromPackage(packageName)
                    "AppUnlock: $appName"
                } ?: "AppUnlock: Unknown"
            }
            else -> null
        }
    }

    private fun generateCalendarEventComment(quest: neth.iecal.questphone.data.quest.CommonQuestInfo?): String? {
        if (quest?.calendar_event_id == null) return null
        
        val schedulingInfo = quest.scheduling_info
        val eventType = when {
            schedulingInfo?.type == SchedulingType.SPECIFIC_DATE -> {
                "One-time event"
            }
            schedulingInfo?.type == SchedulingType.WEEKLY -> {
                val dayName = quest.selected_days.firstOrNull()?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                "Every $dayName"
            }
            schedulingInfo?.type == SchedulingType.MONTHLY_DATE -> {
                "each month"
            }
            schedulingInfo?.type == SchedulingType.MONTHLY_BY_DAY -> {
                val dayName = schedulingInfo.monthlyDayOfWeek?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                val weekInMonth = schedulingInfo.monthlyWeekInMonth
                when {
                    weekInMonth == -1 -> "last $dayName of each month"
                    weekInMonth == 1 -> "Every $dayName"
                    else -> "Every $dayName"
                }
            }
            else -> null
        }
        
        return eventType?.let { "cal: $it" }
    }

    private suspend fun handleEventLogging(previousState: TimerState, currentState: TimerState) {
        eventLoggingMutex.withLock {
            // Skip normal event logging during unplanned breaks
            // Unplanned break events are handled manually in startUnplannedBreak/stopUnplannedBreak
            val latestEvent = questEventDao.getLatestEvent()

            // Skip event logging if the latest event was a checkpoint created recently (within 3 seconds)
            // This prevents race condition between createCheckpoint and handleEventLogging
            val now = System.currentTimeMillis()
            if (latestEvent?.eventName?.startsWith("cp:") == true && 
                now - latestEvent.startTime < 3000) {
                return@withLock
            }

            // Skip normal event logging during unplanned breaks
            // Unplanned break events are handled manually in startUnplannedBreak/stopUnplannedBreak
            if (currentState.mode == TimerMode.UNPLANNED_BREAK || previousState.mode == TimerMode.UNPLANNED_BREAK) {
                return@withLock
            }
            
            val newEventName = getEventName(currentState)

            val isNewEventStarting = newEventName != null
            val isActiveEventRunning = latestEvent != null && latestEvent.endTime == 0L

            if (isNewEventStarting) {
                // If no active event is running, or the new event is different, start a new event sequence.
                if (!isActiveEventRunning || latestEvent?.eventName != newEventName) {
                    var newEventStartTime = now
                    // Close the previous event if it exists and is active.
                    if (isActiveEventRunning) {
                        val closedEvent = latestEvent!!.copy(endTime = now)
                        questEventDao.updateEvent(closedEvent)
                        serviceScope.launch { supabaseSyncService.syncSingleQuestEvent(closedEvent) }
                        newEventStartTime = closedEvent.endTime
                    }

                    // Create the new event.
                    val quest = currentState.activeQuestId?.let { runBlocking { questDao.getQuestById(it) } }
                    val calendarComment = generateCalendarEventComment(quest)
                    val newEvent = QuestEvent(
                        eventName = newEventName!!,
                        startTime = newEventStartTime,
                        endTime = 0L, // Mark as active
                        comments = calendarComment,
                        rewardCoins = currentState.eventDetails?.rewardCoins,
                        preRewardCoins = currentState.eventDetails?.preRewardCoins
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
        UNPLANNED_BREAK,
        UNLOCK_ENDING,
        UNLOCK_PROGRESS
    }

    private fun sendNotification(title: String, message: String, type: NotificationType, progress: Int = 0, maxProgress: Int = 0) {
        val channelId = when (type) {
            NotificationType.UNLOCK_PROGRESS -> "unlock_channel"
            NotificationType.QUEST_OVERDUE, NotificationType.BREAK_OVERDUE, NotificationType.UNPLANNED_BREAK -> "overdue_channel"
            else -> "timer_channel"
        }

        val builder = NotificationCompat.Builder(this, channelId).apply {
            setSmallIcon(android.R.drawable.ic_dialog_info) // Using a default system icon
            setContentTitle(title)
            setContentText(message)

            when (type) {
                NotificationType.UNLOCK_PROGRESS -> {
                    setOngoing(true)
                    setSilent(true)
                    setProgress(maxProgress, progress, false)
                    priority = NotificationCompat.PRIORITY_LOW
                }
                else -> {
                    setSound(getSoundForNotificationType(type))
                    setVibrate(getVibrationPatternForNotificationType(type))
                    priority = NotificationCompat.PRIORITY_HIGH
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                }
            }
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(type.ordinal + 1, builder.build())

        if (type != NotificationType.UNLOCK_PROGRESS) {
            triggerVibration(type)
        }
    }
    
    private fun getSoundForNotificationType(type: NotificationType): Uri {
        val soundResource = when (type) {
            NotificationType.QUEST_ENDING -> neth.iecal.questphone.R.raw.quest_expired
            NotificationType.BREAK_ENDING -> neth.iecal.questphone.R.raw.break_expiried
            NotificationType.QUEST_OVERDUE -> neth.iecal.questphone.R.raw.overdue_quest
            NotificationType.BREAK_OVERDUE -> neth.iecal.questphone.R.raw.overdue_break
            NotificationType.UNPLANNED_BREAK -> neth.iecal.questphone.R.raw.overdue_break
            NotificationType.UNLOCK_ENDING -> neth.iecal.questphone.R.raw.quest_expired // Using the same sound as quest expiry
            NotificationType.UNLOCK_PROGRESS -> neth.iecal.questphone.R.raw.quest_expired // Will be silent due to channel settings
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
            NotificationType.UNLOCK_ENDING -> longArrayOf(0, 300, 100, 300) // Same as QUEST_ENDING
            NotificationType.UNLOCK_PROGRESS -> longArrayOf(0) // No vibration for progress updates
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

    private fun startUnlockTimer(durationMinutes: Int, packageName: String, rewardCoins: Int, preRewardCoins: Int) {
        serviceScope.launch {
            val eventDetails = EventDetails(rewardCoins = rewardCoins, preRewardCoins = preRewardCoins)
            val currentState = _timerState.value

            val unlockDurationMillis = TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
            unlockEndTime = System.currentTimeMillis() + unlockDurationMillis
            currentUnlockDurationMillis = unlockDurationMillis

            // Send intent to AppBlockerService to unlock the app immediately
            val unlockIntent = Intent(INTENT_ACTION_UNLOCK_APP).apply {
                putExtra("package_name", packageName)
                putExtra("selected_time", TimeUnit.MINUTES.toMillis(durationMinutes.toLong()).toInt())
            }
            sendBroadcast(unlockIntent)
            Log.d("TimerService", "Sent unlock intent to AppBlockerService for package: $packageName")

            // If we are in a break or break's overdue, end it first to transition to UNLOCK.
            if (currentState.mode == TimerMode.BREAK || (currentState.mode == TimerMode.OVERTIME && currentState.isBreakOvertime)) {
                breakEndedEarly = true
                breakTerminationTime = System.currentTimeMillis()
                // Get all quests and find the most recently completed one
                val allQuests = questDao.getAllQuestsSuspend()
                val recentlyCompleted = allQuests
                    .filter { it.last_completed_at > 0 }
                    .maxByOrNull { it.last_completed_at }

                if (recentlyCompleted != null) {
                    val overtimeState = TimerState(
                        mode = TimerMode.OVERTIME,
                        time = Duration.ZERO,
                        activeQuestId = recentlyCompleted.id,
                        isBreakOvertime = true,
                        isDeepFocusLocking = false,
                        lastOverdueNotificationTime = System.currentTimeMillis()
                    )
                    preUnlockTimerState = overtimeState

                    val previousStateForLog = _timerState.value
                    _timerState.value = TimerState(
                        mode = TimerMode.UNLOCK,
                        time = Duration.ofMillis(unlockDurationMillis),
                        activeQuestId = recentlyCompleted.id,
                        unlockPackageName = packageName,
                        eventDetails = eventDetails
                    )
                    handleEventLogging(previousStateForLog, _timerState.value)

                    SchedulingUtils.scheduleUnlockWarningNotification(this@TimerService, unlockDurationMillis)
                } else {
                    Log.w("TimerService", "Cannot start unlock timer from break without a recently completed quest.")
                }
            } else {
                preUnlockTimerState = currentState
                _timerState.value = TimerState(
                    mode = TimerMode.UNLOCK,
                    time = Duration.ofMillis(unlockDurationMillis),
                    activeQuestId = currentState.activeQuestId,
                    unlockPackageName = packageName,
                    eventDetails = eventDetails
                )
                handleEventLogging(currentState, _timerState.value)

                SchedulingUtils.scheduleUnlockWarningNotification(this@TimerService, unlockDurationMillis)
            }
            Log.d("TimerService", "Unlock timer started. Ends at: $unlockEndTime")
        }
    }

    private fun endBreakEarly(unlockDuration: Int? = null, packageName: String? = null, eventDetails: EventDetails? = null) {
        Log.d("TimerService", "endBreakEarly method called.")
        serviceScope.launch {
            val currentState = _timerState.value
            Log.d("TimerService", "Current state mode: ${currentState.mode}")
            
            // Only allow early termination if currently in break mode
            if (currentState.mode != TimerMode.BREAK) {
                Log.w("TimerService", "endBreakEarly called, but not in break mode. Aborting.")
                return@launch
            }
            
            val allQuests = questDao.getAllQuestsSuspend()
            val recentlyCompleted = allQuests
                .filter { it.last_completed_at > it.quest_started_at && it.break_duration_minutes > 0 }
                .maxByOrNull { it.last_completed_at }
            
            if (recentlyCompleted != null) {
                breakEndedEarly = true
                breakTerminationTime = System.currentTimeMillis()
                Log.d("TimerService", "Found recently completed quest: ${recentlyCompleted.title}")
                val now = breakTerminationTime!!
                val breakEndsAt = recentlyCompleted.last_completed_at + TimeUnit.MINUTES.toMillis(recentlyCompleted.break_duration_minutes.toLong())
                
                // Check if we're in deep focus mode for locking behavior
                var isLocking = false
                if (recentlyCompleted.integration_id == neth.iecal.questphone.data.IntegrationId.DEEP_FOCUS) {
                    try {
                        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        val deepFocus = json.decodeFromString<neth.iecal.questphone.data.quest.focus.DeepFocus>(recentlyCompleted.quest_json)
                        if (deepFocus.completedWorkSessions < deepFocus.minWorkSessions && deepFocus.completedWorkSessions > 0) {
                            isLocking = true
                        }
                        Log.d("TimerService", "Deep focus locking state: $isLocking")
                    } catch (e: Exception) { 
                        Log.e("TimerService", "Error decoding deep focus JSON", e)
                    }
                }
                
                // Force transition to overdue state with overtime starting at zero
                val overtimeDuration = Duration.ZERO
                Log.d("TimerService", "Forcing transition to overdue state.")
                val previousState = _timerState.value
                _timerState.value = TimerState(
                    mode = TimerMode.OVERTIME, 
                    time = overtimeDuration, 
                    activeQuestId = recentlyCompleted.id, 
                    isBreakOvertime = true, 
                    isDeepFocusLocking = isLocking, 
                    lastOverdueNotificationTime = now
                )
                // Only log the OVERTIME event if we're not about to start an unlock timer
                if (unlockDuration == null || packageName == null) {
                    handleEventLogging(previousState, _timerState.value)
                }
                Log.d("TimerService", "New state set to OVERTIME.")
            } else {
                Log.w("TimerService", "endBreakEarly called, but no recently completed quest found.")
            }

            // If the break was ended by an unlock purchase, start the unlock timer immediately.
            if (unlockDuration != null && packageName != null && unlockDuration > 0 && packageName.isNotEmpty()) {
                startUnlockTimer(unlockDuration, packageName, eventDetails?.rewardCoins ?: 0, eventDetails?.preRewardCoins ?: 0)
            }
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
        const val ACTION_END_BREAK_EARLY = "neth.iecal.questphone.action.END_BREAK_EARLY"
        const val ACTION_START_UNLOCK_TIMER = "neth.iecal.questphone.action.START_UNLOCK_TIMER"
        const val EXTRA_UNLOCK_DURATION = "unlock_duration_minutes"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_REWARD_COINS = "reward_coins"
        const val EXTRA_PRE_REWARD_COINS = "pre_reward_coins"

        const val EXTRA_TIME_TO_ADD = "neth.iecal.questphone.extra.TIME_TO_ADD"

        private const val CHANNEL_ID = "timer_channel"
        private const val VIBRATOR_MANAGER_SERVICE = "vibrator_manager"
    }
}
