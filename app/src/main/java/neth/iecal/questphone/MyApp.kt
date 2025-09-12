package neth.iecal.questphone

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import neth.iecal.questphone.utils.VibrationHelper
import neth.iecal.questphone.data.game.Pet
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.services.reloadServiceInfo
import neth.iecal.questphone.utils.isOnline
import neth.iecal.questphone.utils.triggerQuestSync
import neth.iecal.questphone.utils.triggerStatsSync
import neth.iecal.questphone.utils.CalendarSyncInitializer
import neth.iecal.questphone.utils.DailyResetScheduler
import neth.iecal.questphone.workers.SyncWorker
import java.util.concurrent.TimeUnit

class MyApp : Application() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate() {
        super.onCreate()

        User.init(this)
        VibrationHelper.init(this)
        Pet.init(this)

        reloadServiceInfo(this)
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Trigger sync when network becomes available
                enqueueQuestSync()
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        if (isOnline()) enqueueQuestSync()

        // Initialize calendar sync scheduling
        CalendarSyncInitializer.initialize(applicationContext)

        // Schedule daily reset (coins reset and streak evaluation)
        DailyResetScheduler.schedule(applicationContext)

        // Ensure periodic background sync when connected
        enqueuePeriodicQuestSync()
    }

    private fun enqueueQuestSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "quest-sync-once",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun enqueuePeriodicQuestSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodic = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "quest-sync-periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
    }
}