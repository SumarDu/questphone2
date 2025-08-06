package neth.iecal.questphone.data.remote

import android.content.Context
import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.data.local.QuestEventDao
import neth.iecal.questphone.data.quest.focus.DeepFocusSessionLog
import java.text.SimpleDateFormat
import java.util.*

class SupabaseSyncService(private val context: Context) {

    suspend fun syncSingleQuestEvent(event: neth.iecal.questphone.data.local.QuestEvent) {
        try {
            val eventToSync = QuestEventSupabase(
                id = event.id,
                event_name = event.eventName,
                start_time = event.startTime,
                end_time = if (event.endTime > 0) event.endTime else null,
                comments = event.comments,
                reward_coins = event.rewardCoins,
                pre_reward_coins = event.preRewardCoins
            )
            SupabaseClient.client.postgrest.from("quest_events").upsert(eventToSync, onConflict = "id")
            Log.d("SupabaseSyncService", "Successfully synced single quest event: ${event.id}")
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Error syncing single quest event: ${event.id}", e)
        }
    }


    private val questEventDao: QuestEventDao = AppDatabase.getDatabase(context).questEventDao()

    suspend fun syncDeepFocusLog(log: DeepFocusSessionLog) {
        try {
            SupabaseClient.client.postgrest.from("deep_focus_session_logs").upsert(log, onConflict = "client_uuid")
            Log.d("SupabaseSyncService", "Successfully synced deep focus log: ${log.id}")
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Error syncing deep focus log: ${log.id}", e)
        }
    }

    suspend fun createCheckpoint(checkpointName: String, comments: String? = null) {
        try {
            val now = System.currentTimeMillis()
            val checkpointEndTime = now + 1000 // 1 second duration
            
            // Check if there's an active event that needs to be suspended
            val latestEvent = questEventDao.getLatestEvent()
            var suspendedEventName: String? = null
            
            if (latestEvent != null && latestEvent.endTime == 0L) {
                // Close the active event at checkpoint start time
                val closedEvent = latestEvent.copy(endTime = now)
                questEventDao.updateEvent(closedEvent)
                syncSingleQuestEvent(closedEvent)
                suspendedEventName = latestEvent.eventName
            }
            
            // Create the checkpoint event
            val checkpointEvent = neth.iecal.questphone.data.local.QuestEvent(
                eventName = "cp: $checkpointName",
                startTime = now,
                endTime = checkpointEndTime,
                comments = comments
            )
            
            questEventDao.insertEvent(checkpointEvent)
            syncSingleQuestEvent(checkpointEvent)
            
            // ВАЖЛИВО: Створюємо відновлену подію з startTime = endTime чекпоїнта
            if (suspendedEventName != null && !suspendedEventName.startsWith("cp:") && suspendedEventName != "unplanned break") {
                val resumedEvent = neth.iecal.questphone.data.local.QuestEvent(
                    eventName = suspendedEventName,
                    startTime = checkpointEndTime, // StartTime дорівнює endTime чекпоїнта
                    endTime = 0L, // Active event
                    comments = null
                )
                
                questEventDao.insertEvent(resumedEvent)
                syncSingleQuestEvent(resumedEvent)
            }
            
            Log.d("SupabaseSyncService", "Successfully created checkpoint: $checkpointName")
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Error creating checkpoint: $checkpointName", e)
        }
    }

    fun syncQuestEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            val events = questEventDao.getAllEvents().first()
            val eventsToSync = events.map { event ->
                QuestEventSupabase(
                    id = event.id,
                    event_name = event.eventName,
                    start_time = event.startTime,
                    end_time = if (event.endTime > 0) event.endTime else null,
                    comments = event.comments,
                    reward_coins = event.rewardCoins,
                    pre_reward_coins = event.preRewardCoins
                )
            }
            if (eventsToSync.isNotEmpty()) {
                try {
                    SupabaseClient.client.postgrest.from("quest_events").upsert(eventsToSync, onConflict = "id")
                    Log.d("SupabaseSyncService", "Successfully synced quest events")
                } catch (e: Exception) {
                    Log.e("SupabaseSyncService", "Error syncing quest events", e)
                }
            }
        }
    }
}
