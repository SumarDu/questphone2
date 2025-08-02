package neth.iecal.questphone.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import neth.iecal.questphone.workers.CalendarSyncWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object CalendarSyncScheduler {
    private const val TAG = "CalendarSyncScheduler"
    private const val WORK_NAME = "calendar_sync_work"

    /**
     * Schedule or reschedule calendar sync based on the provided hour
     * @param context Application context
     * @param syncHour Hour of day (0-23) when sync should occur, null to cancel
     */
    fun scheduleSync(context: Context, syncMinutes: Int?) {
        val workManager = WorkManager.getInstance(context)
        
        // Cancel existing work first
        workManager.cancelUniqueWork(WORK_NAME)
        
        if (syncMinutes == null) {
            Log.d(TAG, "Calendar sync disabled")
            return
        }
        
        // Calculate delay until next sync time
        val delay = calculateDelayUntilSyncTime(syncMinutes)
        
        Log.d(TAG, "Scheduling calendar sync for minutes $syncMinutes (delay: ${delay / (1000 * 60)} minutes)")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(CalendarSyncWorker.TAG)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    /**
     * Cancel scheduled calendar sync
     */
    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "Calendar sync cancelled")
    }
    
    /**
     * Check if calendar sync is currently scheduled
     */
    fun isSyncScheduled(context: Context): Boolean {
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        return workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    }
    
    /**
     * Calculate delay in milliseconds until the next occurrence of the specified hour
     */
    private fun calculateDelayUntilSyncTime(syncMinutes: Int): Long {
        val now = Calendar.getInstance()
        val syncTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, syncMinutes / 60)
            set(Calendar.MINUTE, syncMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // If sync time has already passed today, schedule for tomorrow
        if (syncTime.timeInMillis <= now.timeInMillis) {
            syncTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return syncTime.timeInMillis - now.timeInMillis
    }
    
    /**
     * Trigger immediate sync (for testing or manual trigger)
     */
    fun triggerImmediateSync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
            .setConstraints(constraints)
            .addTag("immediate_sync")
            .build()
        
        workManager.enqueue(syncRequest)
        Log.d(TAG, "Immediate calendar sync triggered")
    }
}
