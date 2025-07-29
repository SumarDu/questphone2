package neth.iecal.questphone.data

import kotlinx.serialization.Serializable

@Serializable
enum class SchedulingType {
    WEEKLY,           // Current implementation - specific days of week
    SPECIFIC_DATE,    // One-time quest on specific date
    MONTHLY_DATE,     // Monthly recurring on specific date (e.g., 15th of each month)
    MONTHLY_LAST_DAY  // Last occurrence of specific day in month (e.g., last Saturday)
}

@Serializable
data class SchedulingInfo(
    val type: SchedulingType = SchedulingType.WEEKLY,
    val selectedDays: Set<DayOfWeek> = emptySet(),           // For WEEKLY
    val specificDate: String? = null,                         // For SPECIFIC_DATE (yyyy-MM-dd)
    val monthlyDate: Int? = null,                            // For MONTHLY_DATE (1-31)
    val monthlyLastDayOfWeek: DayOfWeek? = null              // For MONTHLY_LAST_DAY
)
