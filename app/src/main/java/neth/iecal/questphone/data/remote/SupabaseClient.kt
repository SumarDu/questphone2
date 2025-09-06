package neth.iecal.questphone.data.remote

import android.content.Context
import neth.iecal.questphone.BuildConfig
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    @Volatile
    private var instance: JanSupabaseClient? = null

    fun init(context: Context) {
        val useDev = DevModeManager.isActive(context)
        val url = if (useDev) BuildConfig.SUPABASE_URL_DEV else BuildConfig.SUPABASE_URL
        val key = if (useDev) BuildConfig.SUPABASE_KEY_DEV else BuildConfig.SUPABASE_KEY
        // If DEV mode is selected but credentials are missing, do NOT fall back to prod. Disable client.
        if (useDev && (url.isBlank() || key.isBlank())) {
            instance = null
            return
        }
        instance = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    fun client(): JanSupabaseClient {
        return instance ?: throw IllegalStateException("SupabaseClient not initialized. Call SupabaseClient.init(context) early in app startup.")
    }

    fun isAvailable(): Boolean = instance != null
}
