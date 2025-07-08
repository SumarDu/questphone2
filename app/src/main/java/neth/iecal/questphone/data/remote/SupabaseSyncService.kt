package neth.iecal.questphone.data.remote

import android.util.Log
import neth.iecal.questphone.data.quest.focus.DeepFocusSessionLog
import io.github.jan.supabase.postgrest.postgrest

class SupabaseSyncService {

    suspend fun syncDeepFocusLog(log: DeepFocusSessionLog) {
        try {
            SupabaseClient.client.postgrest.from("deep_focus_session_logs").upsert(log, onConflict = "client_uuid")
            Log.d("SupabaseSyncService", "Successfully synced deep focus log: ${log.id}")
        } catch (e: Exception) {
            Log.e("SupabaseSyncService", "Error syncing deep focus log: ${log.id}", e)
        }
    }
}
