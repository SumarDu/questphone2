package neth.iecal.questphone.utils

import neth.iecal.questphone.data.DayOfWeek
import neth.iecal.questphone.data.SchedulingInfo
import neth.iecal.questphone.data.SchedulingType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters

object SchedulingUtils {
    
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
            
            SchedulingType.MONTHLY_LAST_DAY -> {
                schedulingInfo.monthlyLastDayOfWeek?.let { targetDayOfWeek ->
                    val lastOccurrence = getLastOccurrenceOfDayInMonth(date.year, date.month, targetDayOfWeek)
                    date == lastOccurrence
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
            
            SchedulingType.MONTHLY_LAST_DAY -> {
                schedulingInfo.monthlyLastDayOfWeek?.let { targetDayOfWeek ->
                    getNextLastDayOccurrence(targetDayOfWeek, fromDate)
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
            
            SchedulingType.MONTHLY_LAST_DAY -> {
                schedulingInfo.monthlyLastDayOfWeek?.let { dayOfWeek ->
                    val dayName = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
                    "Last $dayName of each month"
                } ?: "No day selected"
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
    
    private fun getNextLastDayOccurrence(targetDayOfWeek: DayOfWeek, fromDate: LocalDate): LocalDate? {
        var currentMonth = YearMonth.from(fromDate)
        
        // Check current month first
        val currentMonthDate = getLastOccurrenceOfDayInMonth(currentMonth.year, currentMonth.month, targetDayOfWeek)
        if (currentMonthDate.isAfter(fromDate)) {
            return currentMonthDate
        }
        
        // Check next months
        repeat(12) {
            currentMonth = currentMonth.plusMonths(1)
            return getLastOccurrenceOfDayInMonth(currentMonth.year, currentMonth.month, targetDayOfWeek)
        }
        
        return null
    }
    
    private fun getLastOccurrenceOfDayInMonth(year: Int, month: java.time.Month, targetDayOfWeek: DayOfWeek): LocalDate {
        val lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth()
        val javaDayOfWeek = targetDayOfWeek.toJavaDayOfWeek()
        return lastDayOfMonth.with(TemporalAdjusters.lastInMonth(javaDayOfWeek))
    }
    
    /**
     * Updates scheduling info for the next occurrence after quest completion.
     * For specific date quests, returns null (should be destroyed).
     * For repeating quests, updates to next occurrence.
     */
    fun updateSchedulingForNextOccurrence(schedulingInfo: SchedulingInfo): SchedulingInfo? {
        return when (schedulingInfo.type) {
            SchedulingType.WEEKLY -> {
                // Weekly quests don't need updating - they repeat on the same days
                schedulingInfo
            }
            
            SchedulingType.SPECIFIC_DATE -> {
                // Specific date quests should be destroyed after completion
                null
            }
            
            SchedulingType.MONTHLY_DATE -> {
                // Monthly date quests don't need updating - they repeat on the same date each month
                schedulingInfo
            }
            
            SchedulingType.MONTHLY_LAST_DAY -> {
                // Monthly last day quests don't need updating - they repeat on the same last day each month
                schedulingInfo
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
            else -> defaultExpiration
        }
    }
}


