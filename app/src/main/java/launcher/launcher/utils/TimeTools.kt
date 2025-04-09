package launcher.launcher.utils

import launcher.launcher.data.DayOfWeek
import java.text.SimpleDateFormat
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

fun formatHour(hour: Int): String {
    return when (hour) {
        0, 24 -> "12 AM" // Midnight fix
        12 -> "12 PM"
        in 1..11 -> "$hour AM"
        else -> "${hour - 12} PM"
    }
}
