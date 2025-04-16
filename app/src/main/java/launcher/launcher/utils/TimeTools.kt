package launcher.launcher.utils

import launcher.launcher.data.DayOfWeek
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * format: yyyy-MM-dd
 */
fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

/**
 * format: yyyy-MM-dd
 */
fun getPreviousDay(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DATE, -1) // Subtract 1 day
    return sdf.format(calendar.time)
}

/**
 * format: yyyy-MM-dd
 */
fun getDateFromString(dateStr: String): Date {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.parse(dateStr) ?: Date()
}

fun getDayName(date: Date): String {
    val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
    return sdf.format(date)
}

/**
 * format: yyyy-MM-dd-HH-mm
 */
fun getFullFormattedTime(): String {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM-HH-mm")
    return current.format(formatter)
}

/**
 * checks time in format: yyyy-MM-dd-HH-mm
 */
fun isTimeOver(targetTimeStr: String): Boolean {
    val formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM-HH-mm")
    return try {
        val targetTime = LocalDateTime.parse(targetTimeStr, formatter)
        val now = LocalDateTime.now()
        now.isAfter(targetTime)
    } catch (e: Exception) {
        e.printStackTrace()
        false // or throw an error if you prefer
    }
}

/**
 * format: yyyy-MM-dd-HH-mm
 */
fun getFullTimeAfter(hoursToAdd: Long, minutesToAdd: Long): String {
    val current = LocalDateTime.now()
    val futureTime = current.plusHours(hoursToAdd).plusMinutes(minutesToAdd)
    val formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM-HH-mm")
    return futureTime.format(formatter)
}

/**
 * converts the time in format: yyyy-dd-MM-HH-mm to a ux friendly string
 * 
 */
fun formatRemainingTime(timeString: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM-HH-mm")
        val endTime = LocalDateTime.parse(timeString, formatter)
        val now = LocalDateTime.now()

        if (endTime.isBefore(now)) return "Already ended"

        val duration = java.time.Duration.between(now, endTime)
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        when {
            totalMinutes <= 1 -> "Ending now"
            hours == 0L       -> "Ends in ${minutes}m"
            minutes == 0L     -> "Ends in ${hours}h"
            else              -> "Ends in ${hours}h ${minutes}m"
        }
    } catch (e: Exception) {
        "Invalid time"
    }
}
fun getCurrentDay(): DayOfWeek {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> DayOfWeek.MON
        Calendar.TUESDAY -> DayOfWeek.TUE
        Calendar.WEDNESDAY -> DayOfWeek.WED
        Calendar.THURSDAY -> DayOfWeek.THU
        Calendar.FRIDAY -> DayOfWeek.FRI
        Calendar.SATURDAY -> DayOfWeek.SAT
        Calendar.SUNDAY -> DayOfWeek.SUN
        else -> throw IllegalStateException("Invalid day of week")
    }
}
fun getDayOfWeekEnum(date: Date): DayOfWeek {
    val calendar = Calendar.getInstance()
    calendar.time = date

    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> DayOfWeek.MON
        Calendar.TUESDAY -> DayOfWeek.TUE
        Calendar.WEDNESDAY -> DayOfWeek.WED
        Calendar.THURSDAY -> DayOfWeek.THU
        Calendar.FRIDAY -> DayOfWeek.FRI
        Calendar.SATURDAY -> DayOfWeek.SAT
        Calendar.SUNDAY -> DayOfWeek.SUN
        else -> throw IllegalStateException("Invalid day")
    }
}

fun java.time.DayOfWeek.convertToDayOfWeek(date: java.time.LocalDate): DayOfWeek{
   return when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> DayOfWeek.MON
        java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUE
        java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WED
        java.time.DayOfWeek.THURSDAY -> DayOfWeek.THU
        java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRI
        java.time.DayOfWeek.SATURDAY -> DayOfWeek.SAT
        java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUN
    }
}


fun formatHour(hour: Int): String {
    return when (hour) {
        0, 24 -> "12 AM" // Midnight fix
        12 -> "12 PM"
        in 1..11 -> "$hour AM"
        else -> "${hour - 12} PM"
    }
}
fun getAllDatesBetween(startDate: Date, endDate: Date): List<Date> {
    val dates = mutableListOf<Date>()
    val cal = Calendar.getInstance()
    cal.time = startDate

    while (!cal.time.after(endDate)) {
        dates.add(cal.time)
        cal.add(Calendar.DATE, 1)
    }
    return dates
}
