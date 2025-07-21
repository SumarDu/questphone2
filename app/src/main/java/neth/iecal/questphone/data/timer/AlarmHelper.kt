package neth.iecal.questphone.data.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmHelper {

    const val ALARM_TYPE = "ALARM_TYPE"
    const val ALARM_TYPE_QUEST_COMPLETE = "QUEST_COMPLETE"
    const val ALARM_TYPE_BREAK_OVER = "BREAK_OVER"
    const val ALARM_TYPE_QUEST_OVERDUE = "QUEST_OVERDUE"
    const val ALARM_TYPE_BREAK_OVERDUE = "BREAK_OVERDUE"
    const val ALARM_TYPE_QUEST_WARNING = "QUEST_WARNING"
    const val ALARM_TYPE_BREAK_WARNING = "BREAK_WARNING"
    const val ALARM_TYPE_OVERDUE_PERIODIC = "OVERDUE_PERIODIC"
    const val ALARM_TYPE_UNPLANNED_BREAK_PERIODIC = "UNPLANNED_BREAK_PERIODIC"

    const val EXTRA_TITLE = "title"
    const val EXTRA_MESSAGE = "message"
    const val EXTRA_QUEST_ID = "quest_id"
    const val EXTRA_END_TIME = "end_time"

        fun setAlarm(
        context: Context,
        timeInMillis: Long,
        requestCode: Int,
        alarmType: String,
        questId: String,
        title: String,
        message: String,
        endTime: Long = 0
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(ALARM_TYPE, alarmType)
            putExtra(EXTRA_QUEST_ID, questId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            if (endTime != 0L) {
                putExtra(EXTRA_END_TIME, endTime)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                // Fallback to inexact alarm if exact alarms permission not granted
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(pendingIntent)
    }
}
