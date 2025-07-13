package neth.iecal.questphone.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.remote.SupabaseSyncService

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val logDao = QuestDatabaseProvider.getInstance(applicationContext).deepFocusSessionLogDao()
        val syncService = SupabaseSyncService(applicationContext)

        val unsyncedLogs = logDao.getUnsyncedLogs()
        if (unsyncedLogs.isEmpty()) {
            Log.d("SyncWorker", "No logs to sync.")
            return Result.success()
        }

        Log.d("SyncWorker", "Found ${unsyncedLogs.size} logs to sync.")

        val successfullySyncedLogIds = mutableListOf<Long>()
        var hasFailures = false

        for (log in unsyncedLogs) {
            try {
                syncService.syncDeepFocusLog(log)
                successfullySyncedLogIds.add(log.id)
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync log with id ${log.id}, will retry later.", e)
                hasFailures = true
            }
        }

        if (successfullySyncedLogIds.isNotEmpty()) {
            try {
                logDao.markAsSynced(successfullySyncedLogIds)
                Log.d("SyncWorker", "Successfully marked ${successfullySyncedLogIds.size} logs as synced.")
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to mark logs as synced.", e)
                // If this fails, the logs will be re-synced next time, which is acceptable.
                hasFailures = true
            }
        }

        return if (hasFailures) {
            Log.d("SyncWorker", "Sync finished with some failures, retrying later.")
            Result.retry()
        } else {
            Log.d("SyncWorker", "Sync finished successfully.")
            Result.success()
        }
    }
}
