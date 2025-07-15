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
                color_rgba = event.colorRgba
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

    fun syncQuestEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            val events = questEventDao.getAllEvents().first()
            val eventsToSync = events.map { event ->
                QuestEventSupabase(
                    id = event.id,
                    event_name = event.eventName,
                    start_time = event.startTime,
                    end_time = if (event.endTime > 0) event.endTime else null,
                    color_rgba = event.colorRgba
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
