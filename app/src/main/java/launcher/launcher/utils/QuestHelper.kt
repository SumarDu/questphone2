package launcher.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.serialization.json.Json
import launcher.launcher.data.quest.BasicQuestInfo
import androidx.core.content.edit
import androidx.room.TypeConverter
import launcher.launcher.data.DayOfWeek
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.OverallStatsUs
import launcher.launcher.data.quest.OverallStats
import launcher.launcher.data.quest.QuestStats
import launcher.launcher.ui.screens.launcher.components.getCurrentTime
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

    val coinHelper = CoinHelper(context)
    val instructionPref: SharedPreferences = context.getSharedPreferences(INSTRUCTION_NAME, Context.MODE_PRIVATE)
    val destructionPref: SharedPreferences = context.getSharedPreferences(DESTRUCTION_NAME, Context.MODE_PRIVATE)

    fun getQuestList(): List<BasicQuestInfo> {
        val serializedList = sharedPreferences.getString(ALL_QUESTS_LIST_KEY, null) ?: return emptyList()

        return try {
            json.decodeFromString<List<BasicQuestInfo>>(serializedList)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveQuestList(list: List<BasicQuestInfo>) {
        val listData = json.encodeToString(list)
        sharedPreferences.edit { putString(ALL_QUESTS_LIST_KEY, listData) }
    }

    inline fun <reified T : Any> appendToQuestList(baseData: BasicQuestInfo, questInfo: T) {
        val currentQuests = getQuestList().toMutableList()
        currentQuests.add(baseData)
        saveQuestList(currentQuests)
        sharedPreferences.edit {
            putString("quest_data_${baseData.title}", json.encodeToString(questInfo))
        }
    }

    fun appendToQuestList(baseData: BasicQuestInfo) {
        val currentQuests = getQuestList().toMutableList()
        currentQuests.add(baseData)
        saveQuestList(currentQuests)
    }

    inline fun <reified T : Any> getQuestInfo(baseData: BasicQuestInfo): T? {
        val questInfo = sharedPreferences.getString("quest_data_${baseData.title}", null) ?: return null
        return try {
            json.decodeFromString<T>(questInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    inline fun <reified T : Any> updateQuestInfo(
        baseData: BasicQuestInfo,
        updateAction: (T) -> T
    ) {
        val data = getQuestInfo<T>(baseData)
        if (data != null) {
            val updatedData = updateAction(data)
            sharedPreferences.edit {
                putString("quest_data_${baseData.title}", json.encodeToString(updatedData))
            }
        }
    }

    fun saveInstruction(title: String, instruction: String) {
        instructionPref.edit { putString(title, instruction) }
    }

    fun getInstruction(title: String): String {
        return instructionPref.getString(title, "").toString()
    }

    fun markAsDestroyed(title: String) {
        destructionPref.edit { putBoolean(title, true) }
    }

    fun isDestroyed(title: String): Boolean {
        return destructionPref.getBoolean(title, false)
    }

    fun isQuestCompleted(title: String, date: String): Boolean? {
        val lastPerformed =
            sharedPreferences.getString(QUEST_LAST_PERFORMED_SUFFIX + title, null) ?: return null
        return lastPerformed == date
    }

    fun markQuestAsComplete(baseData: BasicQuestInfo, date: String) {
        VibrationHelper.vibrate(100)
        checkPreviousFailures(baseData)

        sharedPreferences.edit { putString(QUEST_LAST_PERFORMED_SUFFIX + baseData.title, date) }
        coinHelper.incrementCoinCount(baseData.reward)
        val date = getCurrentDate()

        val totalQuests = filterQuestForToday()
        val completedQuest = totalQuests.filter { data -> isQuestCompleted(data.title, date) == true }
        val overallStats = OverallStats(
            completedQuest.size,
            totalQuests.size
        )
        saveOverallStatsForDate(date, overallStats)
        saveQuestStats(date, QuestStats(true, getCurrentTime()), baseData.title)
    }

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
    fun getQuestStats(baseData: BasicQuestInfo): Map<String, QuestStats> {
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

    fun saveOverallStatsForDate(date: String, overallStats: OverallStats) {
        val sharedPreferences = context.getSharedPreferences("overallStats", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString(
                date, json.encodeToString(overallStats)
            )
        }
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

    /**
     * Filters out all the quests that need to be performed today
     *
     * @param quests
     * @return
     */
    fun filterQuestForToday(quests: List<BasicQuestInfo> = getQuestList()): List<BasicQuestInfo> {
        val today = getCurrentDay()
        Log.d("current day", today.name)
        Log.d("all quests ", quests.toString())

        return quests.filter {
            it.selectedDays.contains(today) && !isDestroyed(it.title)
        }
    }

    /**
     * Returns all quests available on the given date based on selectedDays and autoDestruct.
     */
    fun getQuestsForDay(date: LocalDate): List<BasicQuestInfo> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dayOfWeek = date.dayOfWeek.convertToDayOfWeek()
        return getQuestList().filter {
            it.selectedDays.contains(dayOfWeek) &&
                    !isDestroyed(it.title) &&
                    LocalDate.parse(it.autoDestruct, formatter).isAfter(date)
        }
    }

    /**
     * Returns quests completed on the given date.
     */
    fun getCompletedQuestsForDay(date: LocalDate): List<BasicQuestInfo> {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return getQuestsForDay(date).filter { quest ->
            isQuestCompleted(quest.title, dateStr) == true
        }
    }

    /**
     * Returns quests that were available but not completed on the given date.
     */
    fun getFailedQuestsForDay(date: LocalDate): List<BasicQuestInfo> {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return getQuestsForDay(date).filter { quest ->
            val completed = isQuestCompleted(quest.title, dateStr)
            completed == false || completed == null
        }
    }

    fun checkPreviousFailures(baseData: BasicQuestInfo) {
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


    fun isOver(baseData: BasicQuestInfo): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return currentHour > baseData.timeRange[1]
//            saveQuestStats(getCurrentDate(), QuestStats(false, ""),baseData.title)
    }

    companion object {
        private const val PREF_NAME = "all_quest_preferences"
        private const val ALL_QUESTS_LIST_KEY = "quest_list"
        private const val INSTRUCTION_NAME = "instruction_data"
        private const val DESTRUCTION_NAME = "destruction_data"
        private const val QUEST_LAST_PERFORMED_SUFFIX = "date_wen_quest_lst_d_"
        private const val QUEST_IS_RUNNING_SUFFIX = "quest_state_"

        fun isInTimeRange(baseData: BasicQuestInfo): Boolean {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timeRange = baseData.timeRange
            return currentHour in timeRange[0]..timeRange[1]
        }


        fun isNeedAutoDestruction(baseData: BasicQuestInfo): Boolean {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val autoDestruct = LocalDate.parse(baseData.autoDestruct, formatter)
            return today >= autoDestruct
        }
    }
}

