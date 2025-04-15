package launcher.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.serialization.json.Json
import launcher.launcher.data.quest.BasicQuestInfo
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import launcher.launcher.data.DayOfWeek
import launcher.launcher.data.quest.QuestStatUS
import launcher.launcher.data.quest.QuestStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

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
        sharedPreferences.edit { putString(QUEST_LAST_PERFORMED_SUFFIX + baseData.title, date) }
        coinHelper.incrementCoinCount(baseData.reward)

        val totalQuests = filterQuestForToday()
        val completedQuest = totalQuests.filter { data -> isQuestCompleted(data.title, getCurrentDate()) == true }
        val questStats = QuestStats(
            completedQuest.size,
            totalQuests.size
        )
        saveStatsForDate(getCurrentDate(), questStats)
    }

    fun saveStatsForDate(date: String, questStats: QuestStats) {
        val sharedPreferences = context.getSharedPreferences("questStats", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString(
                date, json.encodeToString(questStats)
            )
        }
    }

    fun getOverallStats(): List<QuestStatUS> {
        val statList: MutableList<QuestStatUS> = mutableListOf()
        val sp = context.getSharedPreferences("questStats", Context.MODE_PRIVATE)
        val allKeys = sp.all.keys

        allKeys.forEach { key ->
            val str = sp.getString(key, null)
            Log.d("stats str", str.toString())
            if (str != null) {
                val value = json.decodeFromString<QuestStats>(str)
                statList.add(QuestStatUS(LocalDate.parse(key, DateTimeFormatter.ofPattern("yyyy-MM-dd")), value.questsPerformed, value.totalQuests))
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
        val dayOfWeek = when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> DayOfWeek.MON
            java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUE
            java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WED
            java.time.DayOfWeek.THURSDAY -> DayOfWeek.THU
            java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRI
            java.time.DayOfWeek.SATURDAY -> DayOfWeek.SAT
            java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUN
        }
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

        fun isOver(baseData: BasicQuestInfo): Boolean {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return (currentHour > baseData.timeRange[1])
        }

        fun isNeedAutoDestruction(baseData: BasicQuestInfo): Boolean {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val autoDestruct = LocalDate.parse(baseData.autoDestruct, formatter)
            return today >= autoDestruct
        }
    }
}