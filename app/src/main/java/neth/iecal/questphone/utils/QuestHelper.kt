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
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return currentHour > baseData.time_range[1]
    }

    companion object {
        private const val PREF_NAME = "all_quest_preferences"
        private const val QUEST_IS_RUNNING_SUFFIX = "quest_state_"

        fun isInTimeRange(baseData: CommonQuestInfo): Boolean {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timeRange = baseData.time_range
            return currentHour in timeRange[0]..timeRange[1]
        }


        fun isNeedAutoDestruction(baseData: CommonQuestInfo): Boolean {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val autoDestruct = LocalDate.parse(baseData.auto_destruct, formatter)
            return today > autoDestruct
        }
    }
}


