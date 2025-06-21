package neth.iecal.questphone.utils

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage

object Supabase {
    const val SUPABASE_URL = "https://hplszhlnchhfwngbojnc.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_zzdik6KGZp6yP9ZGvuqkkA_Qfxy3MLz"
    val supabase by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
            defaultSerializer = KotlinXSerializer(json)
            install(Auth) {
                host = "signup"
                scheme = "blankphone"
//                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
            }
            install(Storage)
            install(Postgrest)
        }
    }
}
