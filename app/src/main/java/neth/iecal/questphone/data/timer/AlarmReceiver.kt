package neth.iecal.questphone.data.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import neth.iecal.questphone.utils.NotificationHelper
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)
        val title = intent.getStringExtra(AlarmHelper.EXTRA_TITLE) ?: return
        val message = intent.getStringExtra(AlarmHelper.EXTRA_MESSAGE) ?: return
        val alarmType = intent.getStringExtra(AlarmHelper.ALARM_TYPE) ?: return
        val questId = intent.getStringExtra(AlarmHelper.EXTRA_QUEST_ID) ?: return

        notificationHelper.showQuestAlertNotification(title, message)

        if (alarmType == AlarmHelper.ALARM_TYPE_QUEST_OVERDUE || alarmType == AlarmHelper.ALARM_TYPE_BREAK_OVERDUE) {
            val endTime = intent.getLongExtra(AlarmHelper.EXTRA_END_TIME, 0)
            if (endTime == 0L) return

            val now = System.currentTimeMillis()
            val overtimeMinutes = TimeUnit.MILLISECONDS.toMinutes(now - endTime)
            val nextNotificationTime = endTime + TimeUnit.MINUTES.toMillis(((overtimeMinutes / 5) + 1) * 5)

            val newTitle = if (alarmType == AlarmHelper.ALARM_TYPE_QUEST_OVERDUE) "Quest Overdue" else "Break Overdue"
            val newMessage = "You've been in overtime for ${((overtimeMinutes / 5) + 1) * 5} minutes."

            AlarmHelper.setAlarm(
                context,
                nextNotificationTime,
                questId.hashCode(),
                alarmType,
                questId,
                newTitle,
                newMessage,
                endTime
            )
        }
    }
}
