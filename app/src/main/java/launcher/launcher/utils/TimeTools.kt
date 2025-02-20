package launcher.launcher.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}
