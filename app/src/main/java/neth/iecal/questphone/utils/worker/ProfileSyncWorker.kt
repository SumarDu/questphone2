package neth.iecal.questphone.utils.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ProfileSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            Log.d("ProfileSyncWorker", "Syncing is disabled.")
            val manager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(1041)
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncError", e.stackTraceToString())
            return Result.failure()
        }
    }
}

