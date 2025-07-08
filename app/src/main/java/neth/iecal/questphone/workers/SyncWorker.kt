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
        val syncService = SupabaseSyncService()

        val unsyncedLogs = logDao.getUnsyncedLogs()
        if (unsyncedLogs.isEmpty()) {
            Log.d("SyncWorker", "No logs to sync.")
            return Result.success()
        }

        Log.d("SyncWorker", "Found ${unsyncedLogs.size} logs to sync.")

        try {
            for (log in unsyncedLogs) {
                syncService.syncDeepFocusLog(log)
                logDao.markAsSynced(listOf(log.id))
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error during sync, will retry.", e)
            return Result.retry()
        }
    }
}
