package launcher.launcher.utils

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.Flow

object Supabase {
    const val SUPABASE_URL = "https://hplszhlnchhfwngbojnc.supabase.co"
    const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhwbHN6aGxuY2hoZnduZ2Jvam5jIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDI1MTE5MTcsImV4cCI6MjA1ODA4NzkxN30.4lK1hS2ZwymniV2W4rxQbVpBJbW1rnsbSw7WwqRUzUk"
    val supabase by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
            defaultSerializer = KotlinXSerializer()
            install(Auth) {
                host = "signup"
                scheme = "blankphone"

                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
            }
            install(Storage)
        }
    }
}
