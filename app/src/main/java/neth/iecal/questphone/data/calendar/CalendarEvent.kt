package neth.iecal.questphone.data.calendar

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.Calendar
import android.util.Log
import neth.iecal.questphone.data.DayOfWeek as AppDayOfWeek
import neth.iecal.questphone.utils.convertToAppDayOfWeek

/**
 * Represents a calendar event that can be converted to a Swift Mark quest
 */
@Entity(tableName = "calendar_events")
@Serializable
data class CalendarEvent(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val calendarId: String,
    val isAllDay: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    /**
     * Parse reward from description field using format "C1D1B1A[prompt]" or "C1-10D1B1A[prompt]" for ranges
     */
    fun parseReward(): Pair<Int, Int> {
        val rewardRange = getRewardRange()
        if (rewardRange != null) {
            return rewardRange
        }

        val regex = Regex("C(\\d+)")
        val match = regex.find(description)
        val singleReward = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return Pair(singleReward, singleReward)
    }
    
    /**
     * Check if reward is a range (e.g., "C1-10")
     */
    fun hasRewardRange(): Boolean {
        return description.contains(Regex("C\\d+-\\d+"))
    }
    
    /**
     * Get reward range as a pair (min, max) or null if not a range
     */
    fun getRewardRange(): Pair<Int, Int>? {
        val rangeRegex = Regex("C(\\d+)-(\\d+)")
        val rangeMatch = rangeRegex.find(description)
        return if (rangeMatch != null) {
            val min = rangeMatch.groupValues[1].toIntOrNull() ?: 0
            val max = rangeMatch.groupValues[2].toIntOrNull() ?: 0
            Pair(min, max)
        } else null
    }

    /**
     * Parse duration from description field using format "C1D1B1A[prompt]" (in minutes)
     */
    fun parseDuration(): Int {
        val regex = Regex("D(\\d+)")
        val result = regex.find(description)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        Log.d("CalendarEvent", "parseDuration: description='$description', result=$result")
        return result
    }

    /**
     * Parse break duration from description field using format "C1D1B1A[prompt]" (in minutes)
     */
    fun parseBreakDuration(): Int {
        val regex = Regex("B(\\d+)")
        val result = regex.find(description)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        Log.d("CalendarEvent", "parseBreakDuration: description='$description', result=$result")
        return result
    }

    /**
     * Parse AI Photo Proof prompt from description field using format "C1D1B1A[prompt]"
     */
    fun parseAIPhotoProofPrompt(): String? {
        val regex = Regex("A\\[([^\\]]+)\\]")
        val result = regex.find(description)?.groupValues?.get(1)
        Log.d("CalendarEvent", "parseAIPhotoProofPrompt: description='$description', result='$result'")
        return result
    }

    /**
     * Check if AI Photo Proof is enabled based on presence of A field
     */
    fun hasAIPhotoProof(): Boolean {
        return description.contains(Regex("A\\["))
    }

    /**
     * Get the date string in yyyy-MM-dd format for quest auto-destruct
     */
    fun getDateString(): String {
        val date = Date(startTime)
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
    
    /**
     * Get the day of week for this event
     */
    fun getDayOfWeek(): AppDayOfWeek {
        val date = Date(startTime)
        val calendar = java.util.Calendar.getInstance()
        calendar.time = date
        return calendar.convertToAppDayOfWeek()
    }
}
