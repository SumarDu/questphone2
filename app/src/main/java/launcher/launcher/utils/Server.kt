package launcher.launcher.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.work.*
import launcher.launcher.utils.worker.QuestSyncWorker

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
fun Context.isOnline(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}



fun triggerSync(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // run only when internet is available
        .build()

    val syncWorkRequest = OneTimeWorkRequestBuilder<QuestSyncWorker>()
        .setConstraints(constraints)
        .build()

    // Enqueue uniquely so multiple calls don’t pile up duplicate workers
    WorkManager.getInstance(context).enqueueUniqueWork(
        "sync_quests_work",
        ExistingWorkPolicy.KEEP,  // if the work is already enqueued or running, keep it and do not enqueue a new one
        syncWorkRequest
    )
}
