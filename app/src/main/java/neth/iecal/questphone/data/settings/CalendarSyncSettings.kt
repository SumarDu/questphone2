package neth.iecal.questphone.data.settings

import kotlinx.serialization.Serializable

@Serializable
data class CalendarSyncSettings(
    val isEnabled: Boolean = false,
    val syncIntervalHours: Int = 24, // Default sync every 24 hours
    val lastSyncTimestamp: Long = 0L,
    val selectedCalendarIds: Set<String> = emptySet(), // Empty means sync all calendars
    val isInitialSyncCompleted: Boolean = false,
    val autoCreateQuests: Boolean = true,
    val questPrefix: String = "Calendar: ",
    val defaultReward: Int = 5,
    val defaultDurationMinutes: Int = 25,
    val defaultBreakMinutes: Int = 5
) {
    companion object {
        val DEFAULT = CalendarSyncSettings()
        
        // Predefined sync intervals
        val SYNC_INTERVALS = mapOf(
            1 to "Every hour",
            6 to "Every 6 hours", 
            12 to "Every 12 hours",
            24 to "Daily",
            48 to "Every 2 days",
            168 to "Weekly"
        )
    }
}
