package launcher.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.serialization.json.Json
import launcher.launcher.data.quest.CommonQuestInfo
import androidx.core.content.edit
import launcher.launcher.data.quest.OverallStatsUs
import launcher.launcher.data.quest.OverallStats
import launcher.launcher.data.quest.QuestStats
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}

class QuestHelper(val context: Context) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )


    fun saveQuestStats(date:String, stats: QuestStats, title: String) {
        val sharedPreferences = context.getSharedPreferences("questStats_$title", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString(
                date, json.encodeToString(stats)
            )
        }
    }
    fun getQuestStats(date:String, title: String): QuestStats? {
        val sharedPreferences = context.getSharedPreferences("questStats_$title", Context.MODE_PRIVATE)
        val str = sharedPreferences.getString(date, null)
        if (str != null) {
            val value = json.decodeFromString<QuestStats>(str)
            return value
        } else {
            return null
        }
    }
    fun getQuestStats(baseData: CommonQuestInfo): Map<String, QuestStats> {
        checkPreviousFailures(baseData)
        val sharedPreferences = context.getSharedPreferences("questStats_${baseData.title}", Context.MODE_PRIVATE)
        val allDates = sharedPreferences.all.keys
        val statsList = mutableMapOf<String, QuestStats>()

        allDates.forEach { key ->
            val str = sharedPreferences.getString(key, null)
            if (str != null) {
                val value = json.decodeFromString<QuestStats>(str)
                statsList[key] = value
            }
        }
        return statsList
    }



    fun getOverallStats(): List<OverallStatsUs> {
        val statList: MutableList<OverallStatsUs> = mutableListOf()
        val sp = context.getSharedPreferences("overallStats", Context.MODE_PRIVATE)
        val allKeys = sp.all.keys

        allKeys.forEach { key ->
            val str = sp.getString(key, null)
            Log.d("stats str", str.toString())
            if (str != null) {
                val value = json.decodeFromString<OverallStats>(str)
                statList.add(OverallStatsUs(LocalDate.parse(key, DateTimeFormatter.ofPattern("yyyy-MM-dd")), value.questsPerformed, value.totalQuests))
            }
        }
        Log.d("stats", statList.toString())
        return statList
    }

    fun isQuestRunning(title: String): Boolean {
        return sharedPreferences.getBoolean(QUEST_IS_RUNNING_SUFFIX + title, false)
    }

    fun setQuestRunning(title: String, isRunning: Boolean) {
        sharedPreferences.edit { putBoolean(QUEST_IS_RUNNING_SUFFIX + title, isRunning) }
    }

    fun checkPreviousFailures(baseData: CommonQuestInfo) {
        val lastPerformedStr = sharedPreferences.getString(
            QUEST_LAST_PERFORMED_SUFFIX + baseData.title, null
        ) ?: baseData.createdOn


        val lastPerformedDate = getDateFromString(lastPerformedStr)
        val yesterday = getDateFromString(if (baseData.selectedDays.contains(getCurrentDay()) && !isInTimeRange(baseData)) getCurrentDate() else getPreviousDay())

        val allDates = getAllDatesBetween(lastPerformedDate, yesterday)

        for (date in allDates) {

            val calendar = Calendar.getInstance()
            calendar.time = date
            val day = calendar.convertToDayOfWeek()
            if (day in baseData.selectedDays) {
                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                val alreadySaved = getQuestStats(formattedDate, baseData.title)
                if (alreadySaved == null) {
                    // Mark as failure
                    saveQuestStats(
                        formattedDate,
                        QuestStats(false,""),
                        baseData.title
                    )
                }
            }
        }
    }


    fun isOver(baseData: CommonQuestInfo): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return currentHour > baseData.timeRange[1]
    }

    companion object {
        private const val PREF_NAME = "all_quest_preferences"
        private const val QUEST_LAST_PERFORMED_SUFFIX = "date_wen_quest_lst_d_"
        private const val QUEST_IS_RUNNING_SUFFIX = "quest_state_"

        fun isInTimeRange(baseData: CommonQuestInfo): Boolean {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timeRange = baseData.timeRange
            return currentHour in timeRange[0]..timeRange[1]
        }


        fun isNeedAutoDestruction(baseData: CommonQuestInfo): Boolean {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val autoDestruct = LocalDate.parse(baseData.autoDestruct, formatter)
            return today >= autoDestruct
        }
    }
}



fun List<CommonQuestInfo>.filterQuestsForToday(){
    val today = getCurrentDay()
    filter {
        it.selectedDays.contains(today) && !it.isDestroyed
    }
}
/**
 * Filter out all quests that need to be performed on a specific day
 */
fun List<CommonQuestInfo>.filterQuestsByDay(date: LocalDate){
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dayOfWeek = date.dayOfWeek.convertToDayOfWeek()
    filter {
        it.selectedDays.contains(dayOfWeek) &&
                !it.isDestroyed &&
                LocalDate.parse(it.autoDestruct, formatter).isAfter(date)
    }
}

/**
 * Filters out all incomplete + failed quests for a specific day
 */
fun filterIncompleteQuestsForDay(quests: List<CommonQuestInfo>, date: LocalDate): List<CommonQuestInfo> {
    quests.filterQuestsByDay(date)
    val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return quests.filter {
        it.lastCompletedOn != dateStr
    }
}

/**
 * Filters out all successful quests for a specific day
 */
fun filterCompleteQuestsForDay(quests: List<CommonQuestInfo>, date: java.time.LocalDate): List<CommonQuestInfo> {
    quests.filterQuestsByDay(date)
    val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return quests.filter {
        it.lastCompletedOn == dateStr
    }
}

