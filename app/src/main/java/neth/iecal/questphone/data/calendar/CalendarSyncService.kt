package neth.iecal.questphone.data.calendar

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.json
import java.util.*
import neth.iecal.questphone.data.DayOfWeek as AppDayOfWeek
import neth.iecal.questphone.utils.convertToAppDayOfWeek

class CalendarSyncService(private val context: Context) {
    
    companion object {
        private const val TAG = "CalendarSyncService"
        private const val CALENDAR_QUEST_PREFIX = ""
    }
    
    private val calendarManager = CalendarManager(context)
    private val questDao = QuestDatabaseProvider.getInstance(context).questDao()
    private val calendarEventDao = QuestDatabaseProvider.getInstance(context).calendarEventDao()
    private val settingsRepository = SettingsRepository(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Perform initial sync - create Swift Mark quests for calendar events in the next 30 days
     */
    suspend fun performInitialSync(): SyncResult {
        try {
            Log.d(TAG, "Starting initial sync...")
            
            // Check calendar permissions first
            if (!calendarManager.hasCalendarPermissions()) {
                Log.w(TAG, "Calendar permissions not granted")
                return SyncResult.PermissionDenied
            }
            
            val settings = settingsRepository.settings.value.calendarSyncSettings
            Log.d(TAG, "Calendar sync enabled: ${settings.isEnabled}")
            
            if (!settings.isEnabled) {
                Log.d(TAG, "Calendar sync is disabled in settings")
                return SyncResult.Error("Calendar sync is disabled")
            }

            // Start from beginning of today, not current time
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startDate = calendar.timeInMillis
            val endDate = startDate + (30 * 24 * 60 * 60 * 1000L) // 30 days from today
            Log.d(TAG, "Fetching events from ${java.util.Date(startDate)} to ${java.util.Date(endDate)}")
            
            val calendarEvents = calendarManager.getCalendarEventsInRange(startDate, endDate, settings.selectedCalendarIds)
            Log.d(TAG, "Found ${calendarEvents.size} calendar events")
            calendarEvents.forEachIndexed { index, event ->
                Log.d(TAG, "Event $index: ${event.title} on ${java.util.Date(event.startTime)}")
            }

            var questsCreated = 0
            calendarEvents.forEach { event ->
                calendarEventDao.insertEvent(event)
                Log.d(TAG, "Processing event: ${event.title}")
                if (createSwiftMarkQuestFromEvent(event)) {
                    questsCreated++
                    Log.d(TAG, "Successfully created quest for event: ${event.title}")
                } else {
                    Log.w(TAG, "Failed to create quest for event: ${event.title}")
                }
            }

            updateLastSyncTime()
            Log.i(TAG, "Created $questsCreated Swift Mark quests from ${calendarEvents.size} calendar events")
            return SyncResult.Success(questsCreated, 0, 0)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during initial sync", e)
            return SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Perform incremental sync - check for changes in the 30-day window
     */
    suspend fun performIncrementalSync(): SyncResult {
        Log.d(TAG, "Starting incremental calendar sync")
        
        if (!calendarManager.hasCalendarPermissions()) {
            return SyncResult.PermissionDenied
        }
        
        try {
            val lastSyncTime = getLastSyncTime()
            
            // Get current calendar events for next 30 days starting from today
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val endTime = startTime + (30 * 24 * 60 * 60 * 1000L) // 30 days from today
            val settings = settingsRepository.settings.value.calendarSyncSettings
            val currentCalendarEvents = calendarManager.getCalendarEventsInRange(startTime, endTime, settings.selectedCalendarIds)
            val currentEventIds = currentCalendarEvents.map { it.id }.toSet()
            
            // Get previously stored events
            val storedEvents = calendarEventDao.getEventsInRange(startTime, endTime)
            val storedEventIds = storedEvents.map { it.id }.toSet()
            
            // Find new events (in current but not in stored)
            val newEventIds = currentEventIds - storedEventIds
            val newEvents = currentCalendarEvents.filter { it.id in newEventIds }
            
            // Find deleted events (in stored but not in current)
            val deletedEventIds = storedEventIds - currentEventIds
            
            // Find modified events
            val modifiedEvents = currentCalendarEvents.filter { current ->
                val stored = storedEvents.find { it.id == current.id }
                stored != null && stored.lastModified < current.lastModified
            }
            
            var questsCreated = 0
            var questsUpdated = 0
            var questsDeleted = 0
            
            // Handle new events
            for (event in newEvents) {
                calendarEventDao.insertEvent(event)
                if (createSwiftMarkQuestFromEvent(event)) {
                    questsCreated++
                }
            }
            
            // Handle deleted events
            for (eventId in deletedEventIds) {
                calendarEventDao.markEventAsDeleted(eventId)
                if (deleteSwiftMarkQuestForEvent(eventId)) {
                    questsDeleted++
                }
            }
            
            // Handle modified events
            for (event in modifiedEvents) {
                calendarEventDao.updateEvent(event)
                if (updateSwiftMarkQuestForEvent(event)) {
                    questsUpdated++
                }
            }
            
            // Update last sync time
            updateLastSyncTime()
            
            Log.d(TAG, "Incremental sync completed: $questsCreated created, $questsUpdated updated, $questsDeleted deleted")
            return SyncResult.Success(questsCreated, questsUpdated, questsDeleted)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during incremental sync", e)
            return SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Create a Swift Mark quest from a calendar event
     */
    private suspend fun createSwiftMarkQuestFromEvent(event: CalendarEvent): Boolean {
        try {
            Log.d(TAG, "Creating quest for event: ${event.title} (ID: ${event.id})")
            val questTitle = CALENDAR_QUEST_PREFIX + event.title
            Log.d(TAG, "Quest title will be: $questTitle")
            
            // Check if quest already exists
            val existingQuest = questDao.getQuest(questTitle)
            if (existingQuest != null) {
                Log.d(TAG, "Quest already exists: $questTitle")
                return false // Quest already exists
            }
            
            val settings = settingsRepository.settings.value.calendarSyncSettings
            
            // Parse values from event description
            val (parsedRewardMin, parsedRewardMax) = event.parseReward()
            val parsedDuration = event.parseDuration()
            val parsedBreakDuration = event.parseBreakDuration()
            val hasAIPhotoProof = event.hasAIPhotoProof()
            val aiPhotoProofPrompt = event.parseAIPhotoProofPrompt() ?: ""
            
            Log.d(TAG, "Parsed values - Reward Min: $parsedRewardMin, Reward Max: $parsedRewardMax, Duration: $parsedDuration min, Break: $parsedBreakDuration min")
            Log.d(TAG, "Default values - Reward: ${settings.defaultReward}, Duration: ${settings.defaultDurationMinutes} min, Break: ${settings.defaultBreakMinutes} min")
            
            // Use parsed values if > 0, otherwise use defaults
            // For quest creation, we'll use the min value as a placeholder
            val rewardMin = if (parsedRewardMin > 0) parsedRewardMin else settings.defaultReward
            val rewardMax = if (parsedRewardMax > 0) parsedRewardMax else settings.defaultReward
            val duration = parsedDuration.takeIf { it > 0 } ?: settings.defaultDurationMinutes
            val breakDuration = parsedBreakDuration.takeIf { it > 0 } ?: settings.defaultBreakMinutes
            
            Log.d(TAG, "Final quest parameters - Reward: $rewardMin-$rewardMax, Duration: $duration min, Break: $breakDuration min, AI Photo Proof: $hasAIPhotoProof")
            
            // Create Swift Mark quest JSON using explicit JSON string construction
            val swiftMarkJson = """
                {
                    "questDurationMinutes": $duration,
                    "breakDurationMinutes": $breakDuration,
                    "aiPhotoProof": $hasAIPhotoProof,
                    "aiPhotoProofDescription": "$aiPhotoProofPrompt"
                }
            """.trimIndent()
            
            Log.d(TAG, "Swift Mark JSON: $swiftMarkJson")
            
            val quest = CommonQuestInfo(
                id = UUID.randomUUID().toString(),
                title = questTitle,
                reward_min = rewardMin,
                reward_max = rewardMax,
                integration_id = IntegrationId.SWIFT_MARK,
                selected_days = emptySet(), // Empty for date-based scheduling
                auto_destruct = event.getDateString(),
                time_range = listOf(0, 24), // All day by default
                created_on = getCurrentDate(),
                quest_duration_minutes = duration,
                break_duration_minutes = breakDuration,
                ai_photo_proof = hasAIPhotoProof,
                ai_photo_proof_description = aiPhotoProofPrompt,
                quest_json = swiftMarkJson,
                synced = false,
                last_updated = System.currentTimeMillis(),
                is_destroyed = false,
                calendar_event_id = event.id
            )
            
            Log.d(TAG, "Attempting to insert quest: ${quest.title}")
            questDao.upsertQuest(quest)
            Log.i(TAG, "Successfully created Swift Mark quest: $questTitle")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating quest for event ${event.id}: ${event.title}", e)
            return false
        }
    }
    
    /**
     * Update a Swift Mark quest based on modified calendar event
     */
    private suspend fun updateSwiftMarkQuestForEvent(event: CalendarEvent): Boolean {
        try {
            val questTitle = CALENDAR_QUEST_PREFIX + event.title
            val existingQuest = questDao.getQuest(questTitle) ?: return false
            
            val settings = settingsRepository.settings.value.calendarSyncSettings
            
            // Parse values from event description
            val (parsedRewardMin, parsedRewardMax) = event.parseReward()
            val parsedDuration = event.parseDuration()
            val parsedBreakDuration = event.parseBreakDuration()
            val hasAIPhotoProof = event.hasAIPhotoProof()
            val aiPhotoProofPrompt = event.parseAIPhotoProofPrompt() ?: ""
            
            Log.d(TAG, "Update - Parsed values - Reward Min: $parsedRewardMin, Reward Max: $parsedRewardMax, Duration: $parsedDuration min, Break: $parsedBreakDuration min")
            
            // Use parsed values if > 0, otherwise use defaults
            // For quest update, we'll use the min value as a placeholder
            val rewardMin = if (parsedRewardMin > 0) parsedRewardMin else settings.defaultReward
            val rewardMax = if (parsedRewardMax > 0) parsedRewardMax else settings.defaultReward
            val duration = parsedDuration.takeIf { it > 0 } ?: settings.defaultDurationMinutes
            val breakDuration = parsedBreakDuration.takeIf { it > 0 } ?: settings.defaultBreakMinutes
            
            Log.d(TAG, "Update - Final quest parameters - Reward: $rewardMin-$rewardMax, Duration: $duration min, Break: $breakDuration min, AI Photo Proof: $hasAIPhotoProof")
            
            // Update Swift Mark quest JSON using explicit JSON string construction
            val swiftMarkJson = """
                {
                    "questDurationMinutes": $duration,
                    "breakDurationMinutes": $breakDuration,
                    "aiPhotoProof": $hasAIPhotoProof,
                    "aiPhotoProofDescription": "$aiPhotoProofPrompt"
                }
            """.trimIndent()
            
            val dayOfWeek = event.getDayOfWeek()
            val updatedQuest = existingQuest.copy(
                reward_min = rewardMin,
                reward_max = rewardMax,
                auto_destruct = event.getDateString(),
                selected_days = emptySet(), // Empty for date-based scheduling
                quest_duration_minutes = duration,
                break_duration_minutes = breakDuration,
                ai_photo_proof = hasAIPhotoProof,
                ai_photo_proof_description = aiPhotoProofPrompt,
                quest_json = swiftMarkJson,
                synced = false,
                last_updated = System.currentTimeMillis()
            )
            
            questDao.upsertQuest(updatedQuest)
            Log.d(TAG, "Updated Swift Mark quest: $questTitle")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating quest for event ${event.id}", e)
            return false
        }
    }
    
    /**
     * Delete a Swift Mark quest for a deleted calendar event
     */
    private suspend fun deleteSwiftMarkQuestForEvent(eventId: String): Boolean {
        try {
            // Find quest by calendar_event_id
            val allQuests = questDao.getAllQuestsSuspend()
            val questToDelete = allQuests.find { it.calendar_event_id == eventId }
            
            if (questToDelete != null) {
                questDao.deleteQuest(questToDelete)
                Log.d(TAG, "Deleted Swift Mark quest for event: $eventId")
                return true
            }
            
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting quest for event $eventId", e)
            return false
        }
    }
    
    private suspend fun getLastSyncTime(): Long {
        // Implementation would depend on how you want to store this
        // For now, return 0 to sync all events
        return 0L
    }
    
    private suspend fun updateLastSyncTime() {
        // Implementation would depend on how you want to store this
        // Could be in SharedPreferences or database
    }
}

/**
 * Result of a calendar sync operation
 */
sealed class SyncResult {
    data class Success(
        val questsCreated: Int,
        val questsUpdated: Int,
        val questsDeleted: Int
    ) : SyncResult()
    
    object PermissionDenied : SyncResult()
    data class Error(val message: String) : SyncResult()
}
