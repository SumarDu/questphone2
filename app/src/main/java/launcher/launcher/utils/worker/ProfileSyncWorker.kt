package launcher.launcher.utils.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import launcher.launcher.R
import launcher.launcher.data.SyncStatus
import launcher.launcher.data.game.User
import launcher.launcher.data.game.UserInfo
import launcher.launcher.data.game.saveUserInfo
import launcher.launcher.utils.Supabase

class ProfileSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {

            val userId = Supabase.supabase.auth.currentUserOrNull()?.id ?: return Result.success()

            Log.d("ProfileSyncManager", "Starting sync for $userId")
            showSyncNotification(applicationContext)
            sendSyncBroadcast(applicationContext, SyncStatus.ONGOING)

            if(User.userInfo.needsSync){
                val profileRemote = Supabase.supabase.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<UserInfo>()

                if (profileRemote != null) {
                    if (profileRemote.last_updated  > User.userInfo.last_updated){
                        User.userInfo = profileRemote
                    }
                }
                Supabase.supabase.postgrest["profiles"].upsert(
                    User.userInfo
                )
                User.userInfo.needsSync = false
                User.saveUserInfo(false)
            }


            return Result.success()
        } catch (e: Exception){
            Log.e("SyncError",e.stackTraceToString())
            return Result.failure()
        }
    }
}

private fun sendSyncBroadcast(context: Context,msg:SyncStatus) {
    val intent = Intent("launcher.launcher.profile_sync")
    intent.putExtra("status", msg.ordinal)
    context.sendBroadcast(intent)
}

private fun showSyncNotification(context: Context) {
    val channelId = "sync_channel"
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Create notification channel (required for Android 8+)
    val channel = NotificationChannel(
        channelId,
        "Sync Status",
        NotificationManager.IMPORTANCE_LOW
    )
    manager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Sync in Progress")
        .setContentText("Your profile is syncing...")
        .setSmallIcon(R.drawable.baseline_info_24)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    manager.notify(1041, notification)
}

