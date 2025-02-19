package launcher.launcher.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import launcher.launcher.Constants
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
            sharedPreferences.getString(QUEST_INFO_SUFFIX + baseData.title, null) ?: return null
        return json.decodeFromString<FocusAppQuestInfo>(questInfo)
    }

    fun getFocusQuestInfo(baseData: BaseQuestInfo): FocusQuestInfo? {
        val questInfo =
            sharedPreferences.getString(QUEST_INFO_SUFFIX + baseData.title, null) ?: return null
        return json.decodeFromString<FocusQuestInfo>(questInfo)
    }


    companion object {
        private const val PREF_NAME = "all_quest_preferences"
        private const val ALL_QUESTS_LIST_KEY = "quest_list"
        private const val QUEST_INFO_SUFFIX = "quest_data_"
    }
}