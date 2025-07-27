package neth.iecal.questphone.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.calendar.CalendarSyncService
import neth.iecal.questphone.data.calendar.SyncResult
import neth.iecal.questphone.data.settings.SettingsRepository
import java.util.concurrent.TimeUnit

class CalendarSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "CalendarSyncWorker"
        const val WORK_NAME = "calendar_sync_work"
        
        fun schedulePeriodicSync(context: Context, intervalHours: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    syncRequest
                )
            
            Log.d(TAG, "Scheduled periodic calendar sync every $intervalHours hours")
        }
        
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled periodic calendar sync")
        }
        
        fun triggerImmediateSync(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
                
            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d(TAG, "Triggered immediate calendar sync")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting calendar sync work")
        
        try {
            val settingsRepository = SettingsRepository(applicationContext)
            val settings = settingsRepository.settings.value
            
            // Check if calendar sync is enabled
            if (!settings.calendarSyncSettings.isEnabled) {
                Log.d(TAG, "Calendar sync is disabled, skipping")
                return@withContext Result.success()
            }
            
            val syncService = CalendarSyncService(applicationContext)
            
            val syncResult = if (settings.calendarSyncSettings.isInitialSyncCompleted) {
                // Perform incremental sync
                syncService.performIncrementalSync()
            } else {
                // Perform initial sync
                val result = syncService.performInitialSync()
                if (result is SyncResult.Success) {
                    // Mark initial sync as completed
                    settingsRepository.updateCalendarSyncSettings(
                        settings.calendarSyncSettings.copy(
                            isInitialSyncCompleted = true,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    )
                }
                result
            }
            
            when (syncResult) {
                is SyncResult.Success -> {
                    Log.d(TAG, "Calendar sync completed successfully: ${syncResult.questsCreated} created, ${syncResult.questsUpdated} updated, ${syncResult.questsDeleted} deleted")
                    
                    // Update last sync timestamp
                    settingsRepository.updateCalendarSyncSettings(
                        settings.calendarSyncSettings.copy(
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    )
                    
                    Result.success()
                }
                
                is SyncResult.PermissionDenied -> {
                    Log.w(TAG, "Calendar sync failed: Permission denied")
                    Result.failure()
                }
                
                is SyncResult.Error -> {
                    Log.e(TAG, "Calendar sync failed: ${syncResult.message}")
                    Result.retry()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during calendar sync", e)
            Result.failure()
        }
    }
}
