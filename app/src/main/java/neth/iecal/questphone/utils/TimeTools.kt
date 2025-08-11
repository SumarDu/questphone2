package neth.iecal.questphone.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import neth.iecal.questphone.data.DayOfWeek as AppDayOfWeek
import java.text.SimpleDateFormat
import java.time.Duration
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
fun formatInstantToDate(instant: Instant): String {
    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return localDate.toString() // yyyy-MM-dd
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

        val duration = Duration.between(now, endTime)
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
fun getCurrentDay(): AppDayOfWeek {
    val calendar = Calendar.getInstance()
    return calendar.convertToAppDayOfWeek()
}

fun Calendar.convertToAppDayOfWeek(): AppDayOfWeek {

    return when ((this.get(Calendar.DAY_OF_WEEK))) {
        Calendar.MONDAY -> AppDayOfWeek.MON
        Calendar.TUESDAY -> AppDayOfWeek.TUE
        Calendar.WEDNESDAY -> AppDayOfWeek.WED
        Calendar.THURSDAY -> AppDayOfWeek.THU
        Calendar.FRIDAY -> AppDayOfWeek.FRI
        Calendar.SATURDAY -> AppDayOfWeek.SAT
        Calendar.SUNDAY -> AppDayOfWeek.SUN
        else -> throw IllegalStateException("Invalid day")
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

// New: format minutes-from-midnight to 12h string (e.g., 0 -> 12 AM, 780 -> 1:00 PM)
fun formatTimeMinutes(totalMinutes: Int): String {
    val minutes = ((totalMinutes % 1440) + 1440) % 1440 // normalize
    val h24 = minutes / 60
    val m = minutes % 60
    val am = h24 < 12 || h24 == 24
    val h12 = when (h24 % 12) {
        0 -> 12
        else -> h24 % 12
    }
    return if (m == 0) {
        "$h12 ${if (am) "AM" else "PM"}"
    } else {
        String.format(Locale.getDefault(), "%d:%02d %s", h12, m, if (am) "AM" else "PM")
    }
}

// New: convert a legacy hour-based range [hStart,hEnd] or minute-based [mStart,mEnd]
// into a pair of minutes (start,end). All-day can be [0,24] or [0,1440].
fun toMinutesRange(range: List<Int>): Pair<Int, Int> {
    if (range.size < 2) return 0 to 1440
    val (a, b) = range[0] to range[1]
    return if (b > 24 || a > 24) {
        // Already in minutes
        a to b
    } else {
        // Legacy hours -> minutes
        val start = a.coerceIn(0, 24) * 60
        val end = b.coerceIn(0, 24) * 60
        start to if (end == 0) 1440 else end
    }
}

// New: recognizes all-day in both hour-based and minute-based ranges
fun isAllDayRange(range: List<Int>): Boolean {
    if (range.size < 2) return false
    val a = range[0]
    val b = range[1]
    return (a == 0 && b == 24) || (a == 0 && b == 1440)
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


fun daysSince(
    dateString: String,
    allowedDays: Set<java.time.DayOfWeek>
): Int {
    val inputDate = LocalDate.parse(dateString)
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val start = minOf(inputDate, today)
    val end = maxOf(inputDate, today)

    var count = 0
    var current = start

    while (current <= end) {
        if (current.dayOfWeek in allowedDays) {
            count++
        }
        current = current.plus(1, DateTimeUnit.DAY)
    }

    return count
}
fun LocalDate.getStartOfWeek(): LocalDate {
    // ISO 8601 week starts on Monday
    val dayOfWeek = this.dayOfWeek.isoDayNumber // Monday = 1, Sunday = 7
    return this.minus(dayOfWeek - 1, DateTimeUnit.DAY)
}

fun calculateMonthsPassedAndRoundedStart(input: Instant): LocalDate?{
    val now = Clock.System.now()

    // Convert both to LocalDate in UTC
    val today = now.toLocalDateTime(TimeZone.UTC).date
    val startDate = input.toLocalDateTime(TimeZone.UTC).date

    // Calculate how many months have passed
    val monthsPassed = ((today.year - startDate.year) * 12) + (today.monthNumber - startDate.monthNumber)

    // If more than 12 months, return rounded start date (next Jan 1)
    val roundedStart = if (monthsPassed > 12) {
        LocalDate(startDate.year + 1, 1, 1)
    } else {
        null // No need to round
    }

    return roundedStart
}