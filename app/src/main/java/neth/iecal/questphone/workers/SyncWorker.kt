package neth.iecal.questphone.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.remote.SupabaseSyncService
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.data.local.QuestEvent

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val logDao = QuestDatabaseProvider.getInstance(applicationContext).deepFocusSessionLogDao()
        val syncService = SupabaseSyncService(applicationContext)
        val questEventDao = AppDatabase.getDatabase(applicationContext).questEventDao()
        var hasFailures = false

        // Reconcile dangling events before syncing: if an event has endTime == 0 and
        // there's a next event, set its endTime to the next event's startTime.
        try {
            reconcileDanglingEvents(questEventDao)
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to reconcile dangling events.", e)
            // Don't fail the worker; proceed to sync what we can
        }

        // Sync quest events
        try {
            val questEventsSyncResult = syncService.syncQuestEvents()
            if (questEventsSyncResult == -1) {
                hasFailures = true
                Log.d("SyncWorker", "Quest events sync had some failures.")
            } else {
                Log.d("SyncWorker", "Successfully synced $questEventsSyncResult quest events.")
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to sync quest events.", e)
            hasFailures = true
        }

        // Sync penalty logs
        try {
            val penaltySyncResult = syncService.syncPenaltyLogs()
            if (penaltySyncResult == -1) {
                hasFailures = true
                Log.d("SyncWorker", "Penalty logs sync had some failures.")
            } else {
                Log.d("SyncWorker", "Successfully synced $penaltySyncResult penalty logs.")
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to sync penalty logs.", e)
            hasFailures = true
        }

        // Sync deep focus logs
        val unsyncedLogs = logDao.getUnsyncedLogs()
        if (unsyncedLogs.isEmpty()) {
            Log.d("SyncWorker", "No deep focus logs to sync.")
        } else {
            Log.d("SyncWorker", "Found ${unsyncedLogs.size} deep focus logs to sync.")

            val successfullySyncedLogIds = mutableListOf<Long>()

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
                    Log.d("SyncWorker", "Successfully marked ${successfullySyncedLogIds.size} deep focus logs as synced.")
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to mark deep focus logs as synced.", e)
                    hasFailures = true
                }
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

    private suspend fun reconcileDanglingEvents(questEventDao: neth.iecal.questphone.data.local.QuestEventDao) {
        val events = questEventDao.getAllEventsList()
        if (events.isEmpty()) return

        var prev: QuestEvent? = null
        for (curr in events) {
            val p = prev
            if (p != null && p.endTime == 0L) {
                // If we have a following event, close the previous at current.startTime
                val inferredEnd = curr.startTime
                if (inferredEnd > p.startTime) {
                    val closed = p.copy(endTime = inferredEnd, synced = false)
                    questEventDao.updateEvent(closed)
                    Log.d("SyncWorker", "Reconciled event endTime id=${p.id} -> ${inferredEnd}")
                }
            }
            prev = curr
        }
        // Do NOT forcibly close the last event; it may still be running legitimately.
    }
}
