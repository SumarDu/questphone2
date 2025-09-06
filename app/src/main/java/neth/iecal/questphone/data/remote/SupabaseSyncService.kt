package neth.iecal.questphone.data.remote

import android.content.Context
import android.util.Log
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.github.jan.supabase.postgrest.postgrest
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.data.local.QuestEventDao
import neth.iecal.questphone.data.local.PenaltyLogDao
import neth.iecal.questphone.data.local.PenaltyLog
import neth.iecal.questphone.data.quest.focus.DeepFocusSessionLog
import java.text.SimpleDateFormat
import java.util.*

class SupabaseSyncService(private val context: Context) {
    init {
        // Ensure Supabase client is initialized with correct mode
        SupabaseClient.init(context)
    }

    suspend fun syncSingleQuestEvent(event: neth.iecal.questphone.data.local.QuestEvent) {
        if (!SupabaseClient.isAvailable()) {
            Log.w("SupabaseSyncService", "Supabase client unavailable (possibly DEV without credentials); skipping single event sync")
            return
        }
        if (!isOnline()) {
            Log.w("SupabaseSyncService", "Device offline, skipping single quest event sync: ${'$'}{event.id}")
            return
        }
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
            SupabaseClient.client().postgrest.from("quest_events").upsert(eventToSync, onConflict = "id")
            
            // Mark as synced in local database
            val updatedEvent = event.copy(synced = true)
            questEventDao.updateEvent(updatedEvent)
            
            Log.d("SupabaseSyncService", "Successfully synced single quest event: ${event.id}")
        } catch (e: Exception) {
            // Network or API error – leave event as unsynced; SyncWorker will retry later.
            Log.e("SupabaseSyncService", "Error syncing single quest event: ${event.id}", e)
        }
    }


    private val questEventDao: QuestEventDao = AppDatabase.getDatabase(context).questEventDao()
    private val penaltyLogDao: PenaltyLogDao = AppDatabase.getDatabase(context).penaltyLogDao()

    suspend fun syncPenaltyLogs(): Int {
        if (!SupabaseClient.isAvailable()) {
            Log.w("SupabaseSyncService", "Supabase client unavailable (possibly DEV without credentials); skipping penalty logs sync")
            return -1
        }
        if (!isOnline()) {
            Log.w("SupabaseSyncService", "Device offline, skipping penalty logs sync")
            return -1
        }
        val unsynced = penaltyLogDao.getUnsynced()
        if (unsynced.isEmpty()) {
            Log.d("SupabaseSyncService", "No penalty logs to sync.")
            return 0
        }
        Log.d("SupabaseSyncService", "Found ${unsynced.size} penalty logs to sync.")
        val okIds = mutableListOf<String>()
        var hasFailures = false
        for (log in unsynced) {
            try {
                val payload = PenaltyLogSupabase(
                    id = log.id,
                    occurred_at = log.occurredAt,
                    amount = log.amount,
                    balance_before = log.balanceBefore,
                    source = log.source,
                    quest_id = log.questId,
                    quest_title = log.questTitle
                )
                SupabaseClient.client().postgrest.from("penalty_logs").upsert(payload, onConflict = "id")
                okIds.add(log.id)
            } catch (e: Exception) {
                Log.e("SupabaseSyncService", "Failed to sync penalty log id=${'$'}{log.id}", e)
                hasFailures = true
            }
        }
        if (okIds.isNotEmpty()) {
            try {
                penaltyLogDao.markAsSynced(okIds)
            } catch (e: Exception) {
                Log.e("SupabaseSyncService", "Failed to mark penalty logs as synced.", e)
                hasFailures = true
            }
        }
        return if (hasFailures) -1 else okIds.size
    }

    suspend fun syncQuestEvents(): Int {
        if (!SupabaseClient.isAvailable()) {
            Log.w("SupabaseSyncService", "Supabase client unavailable (possibly DEV without credentials); skipping batch quest events sync")
            return -1
        }
        if (!isOnline()) {
            Log.w("SupabaseSyncService", "Device offline, skipping batch quest events sync")
            return -1
        }
        val unsyncedEvents = questEventDao.getUnsyncedEvents()
        if (unsyncedEvents.isEmpty()) {
            Log.d("SupabaseSyncService", "No quest events to sync.")
            return 0
        }

        Log.d("SupabaseSyncService", "Found ${unsyncedEvents.size} quest events to sync.")
        val successfullySyncedEventIds = mutableListOf<String>()
        var hasFailures = false

        for (event in unsyncedEvents) {
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
                SupabaseClient.client().postgrest.from("quest_events").upsert(eventToSync, onConflict = "id")
                successfullySyncedEventIds.add(event.id)
            } catch (e: Exception) {
                Log.e("SupabaseSyncService", "Failed to sync quest event with id ${event.id}, will retry later.", e)
                hasFailures = true
            }
        }

        if (successfullySyncedEventIds.isNotEmpty()) {
            try {
                questEventDao.markAsSynced(successfullySyncedEventIds)
                Log.d("SupabaseSyncService", "Successfully marked ${successfullySyncedEventIds.size} quest events as synced.")
            } catch (e: Exception) {
                Log.e("SupabaseSyncService", "Failed to mark quest events as synced.", e)
                hasFailures = true
            }
        }

        return if (hasFailures) -1 else successfullySyncedEventIds.size
    }

    suspend fun syncDeepFocusLog(log: DeepFocusSessionLog) {
        if (!SupabaseClient.isAvailable()) {
            Log.w("SupabaseSyncService", "Supabase client unavailable (possibly DEV without credentials); skipping deep focus log sync")
            return
        }
        if (!isOnline()) {
            Log.w("SupabaseSyncService", "Device offline, skipping deep focus log sync: id=${'$'}{log.id}")
            return
        }
        try {
            SupabaseClient.client().postgrest.from("deep_focus_session_logs").upsert(log, onConflict = "client_uuid")
            Log.d("SupabaseSyncService", "Successfully synced deep focus log: ${log.id}")
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Error syncing deep focus log id=${'$'}{log.id}", e)
        }
    }

    /**
     * If the last two quest_events contain a "cp: dev_m_start" checkpoint and the other
     * event has a NULL end_time, delete that following event and create a "cp: dev_m_end" checkpoint.
     * This is invoked when user switches to Normal Mode to cleanup DEV mode artifacts.
     */
    suspend fun checkAndCleanupDevModeArtifacts(endComment: String? = null) {
        if (!SupabaseClient.isAvailable()) {
            Log.w("SupabaseSyncService", "Supabase unavailable; skip dev cleanup")
            return
        }
        if (!isOnline()) {
            Log.w("SupabaseSyncService", "Offline; skip dev cleanup")
            return
        }
        try {
            val rows = SupabaseClient.client().postgrest
                .from("quest_events")
                .select()
                .decodeList<QuestEventSupabase>()
                .sortedByDescending { it.start_time }
                .take(2)

            if (rows.size < 2) return
            val a = rows[0]
            val b = rows[1]
            val hasStart = (a.event_name == "cp: dev_m_start") || (b.event_name == "cp: dev_m_start")
            if (!hasStart) return
            // Identify the one that follows after the start checkpoint with NULL end_time
            // If sorted DESC, the more recent row is rows[0]. If start is rows[1], then rows[0] follows it; else no-op.
            val candidate = when {
                b.event_name == "cp: dev_m_start" -> a
                a.event_name == "cp: dev_m_start" -> b
                else -> null
            }
            if (candidate != null && candidate.end_time == null && !candidate.event_name.startsWith("cp:")) {
                val idToDelete = candidate.id
                if (!idToDelete.isNullOrBlank()) {
                    runCatching {
                        SupabaseClient.client().postgrest
                            .from("quest_events")
                            .delete { filter { eq("id", idToDelete) } }
                    }.onFailure { Log.w("SupabaseSyncService", "Failed to delete following event id=$idToDelete", it) }
                }
            }
            // Create end checkpoint locally and sync, with user-provided comment
            createCheckpoint("dev_m_end", endComment)
        } catch (e: Exception) {
            Log.w("SupabaseSyncService", "Dev mode cleanup failed", e)
        }
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            val hasValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            hasInternet && hasValidated && hasTransport
        } catch (e: Exception) {
            false
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
                questEventDao.updateEvent(closedEvent.copy(synced = false))
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
}
