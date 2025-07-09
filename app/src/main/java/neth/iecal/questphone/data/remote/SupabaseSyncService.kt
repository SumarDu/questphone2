package neth.iecal.questphone.data.remote

import android.util.Log
import neth.iecal.questphone.data.quest.focus.DeepFocusSessionLog
import neth.iecal.questphone.data.local.WardenEvent
import io.github.jan.supabase.postgrest.postgrest

class SupabaseSyncService {

    suspend fun syncWardenEvent(event: WardenEvent) {
        try {
            SupabaseClient.client.postgrest.from("warden_events").upsert(event, onConflict = "id")
            Log.d("SupabaseSyncService", "Successfully synced warden event: ${event.id}")
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Error syncing warden event: ${event.id}", e)
        }
    }

    suspend fun syncDeepFocusLog(log: DeepFocusSessionLog) {
        try {
            SupabaseClient.client.postgrest.from("deep_focus_session_logs").upsert(log, onConflict = "client_uuid")
            Log.d("SupabaseSyncService", "Successfully synced deep focus log: ${log.id}")
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Error syncing deep focus log: ${log.id}", e)
        }
    }
}
