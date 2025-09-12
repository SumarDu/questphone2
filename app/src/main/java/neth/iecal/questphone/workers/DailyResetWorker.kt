package neth.iecal.questphone.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.saveUserInfo
import neth.iecal.questphone.utils.DailyResetScheduler
import neth.iecal.questphone.utils.getCurrentDate
import java.time.LocalDate
import java.time.ZoneId

class DailyResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext
            val dao = AppDatabase.getDatabase(context).questEventDao()

            // Calculate the time range for yesterday in local time
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val yesterday = today.minusDays(1)

            val startOfYesterday = yesterday.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfYesterday = today.atStartOfDay(zone).toInstant().toEpochMilli() - 1

            val completedCount = dao.countCompletedEventsBetween(startOfYesterday, endOfYesterday)

            // Update streaks based on whether at least one quest was completed yesterday
            val streak = User.userInfo.streak
            if (completedCount > 0) {
                // Only increment if lastCompletedDate is not already today
                if (streak.lastCompletedDate != getCurrentDate()) {
                    // Increment streak and set lastCompletedDate to today
                    streak.currentStreak += 1
                    streak.longestStreak = kotlin.math.max(streak.currentStreak, streak.longestStreak)
                    streak.lastCompletedDate = getCurrentDate()
                    User.saveUserInfo()
                }
            } else {
                // No completion yesterday: reset streaks
                streak.longestStreak = kotlin.math.max(streak.currentStreak, streak.longestStreak)
                streak.currentStreak = 0
                User.saveUserInfo()
            }

            // Reset coin balance at the end of each day
            if (User.userInfo.coins != 0) {
                User.userInfo.coins = 0
                User.saveUserInfo()
            }

            // Schedule next run
            DailyResetScheduler.scheduleNext(context)

            Result.success()
        } catch (e: Exception) {
            Log.d("DailyResetWorker", "Failed: ${e.message}")
            Result.retry()
        }
    }
}
