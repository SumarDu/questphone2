package neth.iecal.questphone.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import neth.iecal.questphone.utils.SchedulingUtils

/**
 * Broadcast receiver that shows a notification when the unlock period is about to end.
 * This is triggered by the alarm set in SchedulingUtils.scheduleUnlockWarningNotification().
 */
class UnlockWarningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Show the unlock warning notification
        SchedulingUtils.showUnlockWarningNotification(context)
    }
}
