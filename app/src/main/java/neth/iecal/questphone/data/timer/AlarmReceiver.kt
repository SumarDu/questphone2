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

        when (alarmType) {
            AlarmHelper.ALARM_TYPE_QUEST_OVERDUE, AlarmHelper.ALARM_TYPE_BREAK_OVERDUE -> {
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
            
            AlarmHelper.ALARM_TYPE_OVERDUE_PERIODIC -> {
                android.util.Log.d("AlarmReceiver", "Received periodic overdue notification for quest $questId with title: $title")
                
                // Schedule next overdue notification in 3 minutes
                val now = System.currentTimeMillis()
                val nextNotificationTime = now + TimeUnit.MINUTES.toMillis(3)
                
                // Determine if this is quest or break overdue based on the original notification
                val isBreakOverdue = title.contains("Break", ignoreCase = true)
                val newTitle = if (isBreakOverdue) "Break Still Overdue" else "Quest Still Overdue"
                val newMessage = if (isBreakOverdue) 
                    "You are still taking too long of a break. Please return to work." 
                else 
                    "You are still overdue on your quest. Please complete it."
                
                android.util.Log.d("AlarmReceiver", "Scheduling next periodic overdue notification at $nextNotificationTime")
                AlarmHelper.setAlarm(
                    context,
                    nextNotificationTime,
                    questId.hashCode() + 1000, // Different request code
                    AlarmHelper.ALARM_TYPE_OVERDUE_PERIODIC,
                    questId,
                    newTitle,
                    newMessage,
                    0
                )
            }
            
            AlarmHelper.ALARM_TYPE_UNPLANNED_BREAK_PERIODIC -> {
                // Schedule next unplanned break notification in 5 minutes
                val now = System.currentTimeMillis()
                val nextNotificationTime = now + TimeUnit.MINUTES.toMillis(5)
                
                AlarmHelper.setAlarm(
                    context,
                    nextNotificationTime,
                    questId.hashCode() + 2000, // Different request code
                    AlarmHelper.ALARM_TYPE_UNPLANNED_BREAK_PERIODIC,
                    questId,
                    "Unplanned Break Continues",
                    "You are still on an unplanned break. Consider returning to your task.",
                    0
                )
            }
        }
    }
}
