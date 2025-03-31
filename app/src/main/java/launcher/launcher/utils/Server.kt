package launcher.launcher.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

suspend fun fetchUrlContent(url: String): String? {
    val okHttpClient = OkHttpClient()
    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.body?.string()
        } catch (e: Exception) {
            Log.d("Error fetching url", e.toString())
            null // Return null on error
        }
    }
}