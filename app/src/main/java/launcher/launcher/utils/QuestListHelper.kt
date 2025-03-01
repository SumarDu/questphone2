package launcher.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.icu.text.CaseMap.Title
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import launcher.launcher.models.quest.BaseQuestInfo
import launcher.launcher.models.quest.FocusAppQuestInfo
import launcher.launcher.models.quest.FocusQuestInfo

class QuestListHelper(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun getQuestList(): List<BaseQuestInfo> {
        val serializedList = sharedPreferences.getString(ALL_QUESTS_LIST_KEY, null) ?: return emptyList()

        return try {
            val wrappers = json.decodeFromString<List<BaseQuestInfo>>(serializedList)
            return wrappers
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    fun saveQuestList(list: List<BaseQuestInfo>){
        val listData = json.encodeToString<List<BaseQuestInfo>>(list)
        sharedPreferences.edit().putString(ALL_QUESTS_LIST_KEY,listData).apply()
    }

    fun appendFocusQuest(baseData: BaseQuestInfo, questInfo: FocusQuestInfo) {
        val currentQuests = getQuestList().toMutableList()
        currentQuests.add(baseData)
        saveQuestList(currentQuests)

        val sp = sharedPreferences.edit().putString("quest_data_"+baseData.title,json.encodeToString(questInfo)).apply()
    }
    fun appendFocusAppQuest(baseData: BaseQuestInfo, questInfo: FocusAppQuestInfo) {
        val currentQuests = getQuestList().toMutableList()
        currentQuests.add(baseData)
        saveQuestList(currentQuests)

        sharedPreferences.edit().putString("quest_data_"+baseData.title,json.encodeToString(questInfo)).apply()
    }

    fun getAppFocusQuestInfo(baseData: BaseQuestInfo): FocusAppQuestInfo? {
        val questInfo =
            sharedPreferences.getString(QUEST_MORE_INFO_SUFFIX + baseData.title, null) ?: return null
        return json.decodeFromString<FocusAppQuestInfo>(questInfo)
    }

    fun getFocusQuestInfo(baseData: BaseQuestInfo): FocusQuestInfo? {
        val questInfo =
            sharedPreferences.getString(QUEST_MORE_INFO_SUFFIX + baseData.title, null) ?: return null
        return json.decodeFromString<FocusQuestInfo>(questInfo)
    }
    fun updateFocusQuestNextDuration(baseData: BaseQuestInfo){
        val data = getFocusQuestInfo(baseData)
        if (data != null) {
            data.nextFocusDuration = data.focusTimeConfig.incrementTimeInMs
            sharedPreferences.edit().putString("quest_data_"+baseData.title,json.encodeToString(data)).apply()
        }
    }
    fun updateAppFocusQuestNextDuration(baseData: BaseQuestInfo){
        val data = getFocusQuestInfo(baseData)
        if (data != null) {
            data.nextFocusDuration = data.focusTimeConfig.incrementTimeInMs
            sharedPreferences.edit().putString("quest_data_"+baseData.title,json.encodeToString(data)).apply()
        }
    }

    fun isQuestCompleted(title:String, date: String): Boolean? {
        val lastPerformed =
            sharedPreferences.getString(QUEST_LAST_PERFORMED_SUFFIX + title, null) ?: return null

        return lastPerformed == date
    }

    fun setComplete(title:String, date: String, isComplete: Boolean) {
        sharedPreferences.edit().putString(QUEST_LAST_PERFORMED_SUFFIX + title, date).apply()
    }


    fun filterQuestsForToday(quests: List<BaseQuestInfo>): List<BaseQuestInfo> {
        val today = getCurrentDay() // Get today's enum value
        Log.d("current day",today.name)
        Log.d("all quests ",quests.toString())
        return quests.filter { it.selectedDays.contains(today) }
    }

    companion object {
        private const val PREF_NAME = "all_quest_preferences"
        private const val ALL_QUESTS_LIST_KEY = "quest_list"
        private const val QUEST_MORE_INFO_SUFFIX = "quest_data_"
        private const val QUEST_LAST_PERFORMED_SUFFIX = "date_wen_quest_lst_d_"

    }
}