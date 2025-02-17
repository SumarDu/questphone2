package launcher.launcher.utils


/*
Convert a passed value to milliseconds. Unit can either be h or m.
 */
fun TimeToMillis(value: Long, unit: String): Long {
    return when (unit.lowercase()) {
        "h" -> value * 60L * 60 * 1000  // Convert hours to milliseconds
        "m" -> value * 60L * 1000       // Convert minutes to milliseconds
        else -> throw IllegalArgumentException("Invalid unit: Use 'h' for hours or 'm' for minutes")
    }
}

/*
Convert a passed value in milliseconds to hours(h) or minutes (m)
 */
fun MillisToTime(millis: Long, unit: String): Int {
    return when (unit.lowercase()) {
        "h" -> (millis / (60 * 60 * 1000)).toInt()  // Convert milliseconds to hours
        "m" -> (millis / (60 * 1000)).toInt()       // Convert milliseconds to minutes
        else -> throw IllegalArgumentException("Invalid unit: Use 'h' for hours or 'm' for minutes")
    }
}