package neth.iecal.questphone.utils

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import neth.iecal.questphone.workers.DailyResetWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object DailyResetScheduler {
    private const val TAG = "DailyResetScheduler"
    private const val WORK_NAME = "daily_reset_work"

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val delay = calculateDelayUntilNextMidnight()
        Log.d(TAG, "Scheduling daily reset (delay: ${delay / (1000 * 60)} minutes)")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<DailyResetWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleNext(context: Context) {
        // For clarity/possible one-off reschedule we simply call schedule
        schedule(context)
    }

    private fun calculateDelayUntilNextMidnight(): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5) // small buffer after midnight
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return next.timeInMillis - now.timeInMillis
    }
}
