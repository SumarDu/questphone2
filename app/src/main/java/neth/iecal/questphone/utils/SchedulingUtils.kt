package neth.iecal.questphone.utils

import neth.iecal.questphone.data.DayOfWeek
import neth.iecal.questphone.data.SchedulingInfo
import neth.iecal.questphone.data.SchedulingType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek as JavaDayOfWeek


import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import neth.iecal.questphone.R
import neth.iecal.questphone.receivers.UnlockWarningReceiver
import java.util.concurrent.TimeUnit

object SchedulingUtils {

    private const val UNLOCK_WARNING_NOTIFICATION_ID = 1001
    private const val UNLOCK_WARNING_CHANNEL_ID = "unlock_warning_channel"

    fun scheduleUnlockWarningNotification(context: Context, unlockDurationMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Create an intent for the broadcast receiver
        val intent = Intent(context, UnlockWarningReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            UNLOCK_WARNING_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate trigger time (1 minute before unlock ends)
        val triggerTime = System.currentTimeMillis() + unlockDurationMillis - TimeUnit.MINUTES.toMillis(1)

        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
        
        // Create notification channel (required for Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UNLOCK_WARNING_CHANNEL_ID,
                context.getString(R.string.unlock_warning_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.unlock_warning_channel_description)
                // Set default sound from res/raw
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val uri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.quest_expired)
                setSound(uri, attrs)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUnlockWarningNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, UNLOCK_WARNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_timer_24)  // Using existing timer icon
            .setContentTitle(context.getString(R.string.unlock_warning_title))
            .setContentText(context.getString(R.string.unlock_warning_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(UNLOCK_WARNING_NOTIFICATION_ID, notification)
    }

    /**
     * Checks if a quest should be available on the given date based on its scheduling info
     */
    fun isQuestAvailableOnDate(schedulingInfo: SchedulingInfo, date: LocalDate): Boolean {
        return when (schedulingInfo.type) {
            SchedulingType.WEEKLY -> {
                val dayOfWeek = date.dayOfWeek.convertToAppDayOfWeek()
                dayOfWeek in schedulingInfo.selectedDays
            }

            SchedulingType.SPECIFIC_DATE -> {
                schedulingInfo.specificDate?.let { dateString ->
                    try {
                        val targetDate = LocalDate.parse(dateString)
                        date == targetDate
                    } catch (e: DateTimeParseException) {
                        false
                    }
                } ?: false
            }

            SchedulingType.MONTHLY_DATE -> {
                schedulingInfo.monthlyDate?.let { targetDay ->
                    date.dayOfMonth == targetDay
                } ?: false
            }

            SchedulingType.MONTHLY_BY_DAY -> {
                schedulingInfo.monthlyDayOfWeek?.let { dayOfWeek ->
                    schedulingInfo.monthlyWeekInMonth?.let { weekInMonth ->
                        val occurrence = getNthOccurrenceOfDayInMonth(date.year, date.month, weekInMonth, dayOfWeek)
                        date == occurrence
                    }
                } ?: false
            }
        }
    }

    /**
     * Gets the next date when a quest will be available
     */
    fun getNextAvailableDate(schedulingInfo: SchedulingInfo, fromDate: LocalDate = LocalDate.now()): LocalDate? {
        return when (schedulingInfo.type) {
            SchedulingType.WEEKLY -> {
                getNextWeeklyOccurrence(schedulingInfo.selectedDays, fromDate)
            }

            SchedulingType.SPECIFIC_DATE -> {
                schedulingInfo.specificDate?.let { dateString ->
                    try {
                        val targetDate = LocalDate.parse(dateString)
                        if (targetDate.isAfter(fromDate)) targetDate else null
                    } catch (e: DateTimeParseException) {
                        null
                    }
                }
            }

            SchedulingType.MONTHLY_DATE -> {
                schedulingInfo.monthlyDate?.let { targetDay ->
                    getNextMonthlyDateOccurrence(targetDay, fromDate)
                }
            }

            SchedulingType.MONTHLY_BY_DAY -> {
                schedulingInfo.monthlyDayOfWeek?.let { dayOfWeek ->
                    schedulingInfo.monthlyWeekInMonth?.let { weekInMonth ->
                        getNextMonthlyByDayOccurrence(weekInMonth, dayOfWeek, fromDate)
                    }
                }
            }
        }
    }

    /**
     * Gets a human-readable description of the scheduling pattern
     */
    fun getSchedulingDescription(schedulingInfo: SchedulingInfo): String {
        return when (schedulingInfo.type) {
            SchedulingType.WEEKLY -> {
                if (schedulingInfo.selectedDays.isEmpty()) {
                    "No days selected"
                } else {
                    val dayNames = schedulingInfo.selectedDays.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                    "Every ${dayNames.joinToString(", ")}"
                }
            }

            SchedulingType.SPECIFIC_DATE -> {
                schedulingInfo.specificDate?.let { dateString ->
                    try {
                        val date = LocalDate.parse(dateString)
                        "On ${date.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                    } catch (e: DateTimeParseException) {
                        "Invalid date"
                    }
                } ?: "No date selected"
            }

            SchedulingType.MONTHLY_DATE -> {
                schedulingInfo.monthlyDate?.let { day ->
                    val suffix = when {
                        day in 11..13 -> "th"
                        day % 10 == 1 -> "st"
                        day % 10 == 2 -> "nd"
                        day % 10 == 3 -> "rd"
                        else -> "th"
                    }
                    "On the $day$suffix of each month"
                } ?: "No date selected"
            }

            SchedulingType.MONTHLY_BY_DAY -> {
                val weekInMonthStr = when (schedulingInfo.monthlyWeekInMonth) {
                    1 -> "first"
                    2 -> "second"
                    3 -> "third"
                    4 -> "fourth"
                    -1 -> "last"
                    else -> ""
                }
                val dayOfWeekStr = schedulingInfo.monthlyDayOfWeek?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""

                if (weekInMonthStr.isNotEmpty() && dayOfWeekStr.isNotEmpty()) {
                    "On the $weekInMonthStr $dayOfWeekStr of each month"
                } else {
                    "No day selected"
                }
            }
        }
    }

    private fun getNextWeeklyOccurrence(selectedDays: Set<DayOfWeek>, fromDate: LocalDate): LocalDate? {
        if (selectedDays.isEmpty()) return null

        var currentDate = fromDate.plusDays(1)
        repeat(7) {
            val dayOfWeek = currentDate.dayOfWeek.convertToAppDayOfWeek()
            if (dayOfWeek in selectedDays) {
                return currentDate
            }
            currentDate = currentDate.plusDays(1)
        }
        return null
    }

    private fun getNextMonthlyDateOccurrence(targetDay: Int, fromDate: LocalDate): LocalDate? {
        var currentMonth = YearMonth.from(fromDate)

        // Check current month first
        val currentMonthDate = try {
            currentMonth.atDay(targetDay)
        } catch (e: Exception) {
            null
        }

        if (currentMonthDate != null && currentMonthDate.isAfter(fromDate)) {
            return currentMonthDate
        }

        // Check next months
        repeat(12) {
            currentMonth = currentMonth.plusMonths(1)
            try {
                return currentMonth.atDay(targetDay)
            } catch (e: Exception) {
                // Day doesn't exist in this month (e.g., Feb 30), continue to next month
            }
        }

        return null
    }

    private fun getNextMonthlyByDayOccurrence(weekInMonth: Int, targetDayOfWeek: DayOfWeek, fromDate: LocalDate): LocalDate? {
        var currentMonth = YearMonth.from(fromDate)

        // Check current month
        val currentMonthOccurrence = getNthOccurrenceOfDayInMonth(currentMonth.year, currentMonth.month, weekInMonth, targetDayOfWeek)
        if (currentMonthOccurrence != null && currentMonthOccurrence.isAfter(fromDate)) {
            return currentMonthOccurrence
        }

        // Check subsequent months
        for (i in 1..12) {
            currentMonth = currentMonth.plusMonths(1)
            val nextOccurrence = getNthOccurrenceOfDayInMonth(currentMonth.year, currentMonth.month, weekInMonth, targetDayOfWeek)
            if (nextOccurrence != null) {
                return nextOccurrence
            }
        }
        return null
    }
    
    private fun getNthOccurrenceOfDayInMonth(year: Int, month: java.time.Month, weekInMonth: Int, targetDayOfWeek: DayOfWeek): LocalDate? {
        val javaDayOfWeek = targetDayOfWeek.toJavaDayOfWeek()
        val yearMonth = YearMonth.of(year, month)

        return if (weekInMonth > 0) {
            val firstDayOfMonth = yearMonth.atDay(1)
            var date = if (firstDayOfMonth.dayOfWeek == javaDayOfWeek) {
                firstDayOfMonth
            } else {
                firstDayOfMonth.with(TemporalAdjusters.next(javaDayOfWeek))
            }
            date = date.plusWeeks((weekInMonth - 1).toLong())
            if (date.month == month) date else null
        } else if (weekInMonth == -1) {
            val lastDayOfMonth = yearMonth.atEndOfMonth()
            lastDayOfMonth.with(TemporalAdjusters.lastInMonth(javaDayOfWeek))
        } else {
            null
        }
    }

    /**
     * Updates scheduling info for the next occurrence after quest completion.
     * For specific date quests, returns null (should be destroyed).
     * For repeating quests, updates to next occurrence.
     */
    fun updateSchedulingForNextOccurrence(schedulingInfo: SchedulingInfo): SchedulingInfo? {
        return when (schedulingInfo.type) {
            SchedulingType.WEEKLY,
            SchedulingType.MONTHLY_DATE,
            SchedulingType.MONTHLY_BY_DAY -> {
                // Repeating quests don't need updating
                schedulingInfo
            }

            SchedulingType.SPECIFIC_DATE -> {
                // Specific date quests should be destroyed after completion
                null
            }
        }
    }

    /**
     * Gets the expiration date for a quest based on its scheduling type.
     * Specific date quests expire the day after their scheduled date.
     * Other quests use the default expiration.
     */
    fun getExpirationDate(schedulingInfo: SchedulingInfo, defaultExpiration: String = "9999-12-31"): String {
        return when (schedulingInfo.type) {
            SchedulingType.SPECIFIC_DATE -> {
                schedulingInfo.specificDate?.let { dateString ->
                    try {
                        val targetDate = LocalDate.parse(dateString)
                        val expirationDate = targetDate.plusDays(1)
                        expirationDate.toString()
                    } catch (e: DateTimeParseException) {
                        defaultExpiration
                    }
                } ?: defaultExpiration
            }
            SchedulingType.WEEKLY,
            SchedulingType.MONTHLY_DATE,
            SchedulingType.MONTHLY_BY_DAY -> defaultExpiration
        }
    }
}

fun JavaDayOfWeek.convertToAppDayOfWeek(): DayOfWeek {
    return when (this) {
        JavaDayOfWeek.MONDAY -> DayOfWeek.MON
        JavaDayOfWeek.TUESDAY -> DayOfWeek.TUE
        JavaDayOfWeek.WEDNESDAY -> DayOfWeek.WED
        JavaDayOfWeek.THURSDAY -> DayOfWeek.THU
        JavaDayOfWeek.FRIDAY -> DayOfWeek.FRI
        JavaDayOfWeek.SATURDAY -> DayOfWeek.SAT
        JavaDayOfWeek.SUNDAY -> DayOfWeek.SUN
    }
}

fun DayOfWeek.toJavaDayOfWeek(): JavaDayOfWeek {
    return when (this) {
        DayOfWeek.MON -> JavaDayOfWeek.MONDAY
        DayOfWeek.TUE -> JavaDayOfWeek.TUESDAY
        DayOfWeek.WED -> JavaDayOfWeek.WEDNESDAY
        DayOfWeek.THU -> JavaDayOfWeek.THURSDAY
        DayOfWeek.FRI -> JavaDayOfWeek.FRIDAY
        DayOfWeek.SAT -> JavaDayOfWeek.SATURDAY
        DayOfWeek.SUN -> JavaDayOfWeek.SUNDAY
    }
}



