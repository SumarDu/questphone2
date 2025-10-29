package neth.iecal.questphone.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.json.Json
import neth.iecal.questphone.data.quest.CommonQuestInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
    encodeDefaults = true
    explicitNulls = false
}

class QuestHelper(val context: Context) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun isQuestRunning(title: String): Boolean {
        return sharedPreferences.getBoolean(QUEST_IS_RUNNING_SUFFIX + title, false)
    }

    fun setQuestRunning(title: String, isRunning: Boolean) {
        sharedPreferences.edit { putBoolean(QUEST_IS_RUNNING_SUFFIX + title, isRunning) }
    }

    fun isOver(baseData: CommonQuestInfo): Boolean {
        // Only the daily deadline determines failure; time ranges are informational
        val deadlineMinutes = baseData.deadline_minutes
        if (deadlineMinutes < 0) return false
        val cal = Calendar.getInstance()
        val hours = deadlineMinutes / 60
        val minutes = deadlineMinutes % 60
        cal.set(Calendar.HOUR_OF_DAY, hours)
        cal.set(Calendar.MINUTE, minutes)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val deadlineMillis = cal.timeInMillis
        val nowMillis = System.currentTimeMillis()
        return nowMillis > deadlineMillis
    }

    companion object {
        private const val PREF_NAME = "all_quest_preferences"
        private const val QUEST_IS_RUNNING_SUFFIX = "quest_state_"

        fun isInTimeRange(baseData: CommonQuestInfo): Boolean {
            val cal = Calendar.getInstance()
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val (start, end) = toMinutesRange(baseData.time_range)
            
            // Handle overnight time ranges (e.g., 23:00 to 07:00)
            return if (end < start) {
                // Overnight range: current time is in range if >= start OR <= end
                nowMinutes >= start || nowMinutes <= end
            } else {
                // Normal range: current time is between start and end
                nowMinutes in start..end
            }
        }


        fun isNeedAutoDestruction(baseData: CommonQuestInfo): Boolean {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val autoDestruct = LocalDate.parse(baseData.auto_destruct, formatter)
            return today > autoDestruct
        }
    }
}


