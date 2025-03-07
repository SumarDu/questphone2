package launcher.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import launcher.launcher.data.quest.BaseQuest

/* How quests are stored:
Each quest has a Base Quest object that contains essential details like the title. This basic data is required for all types of quests.

- All BasicQuestInfo objects are stored in a list under the key `ALL_QUEST_LIST_KEY`.
- Each Quest has a unique title in this list.
- Quest-specific data (such as selected apps, focus time settings, etc.) is stored separately under the key `"quest_data_$title"`.
*/

class QuestHelper(context: Context) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun getQuestList(): List<BaseQuest> {
        val serializedList = sharedPreferences.getString(ALL_QUESTS_LIST_KEY, null) ?: return emptyList()

        return try {
            val wrappers = json.decodeFromString<List<BaseQuest>>(serializedList)
            return wrappers
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    fun saveQuestList(list: List<BaseQuest>){
        val listData = json.encodeToString<List<BaseQuest>>(list)
        sharedPreferences.edit().putString(ALL_QUESTS_LIST_KEY,listData).apply()
    }


     inline fun <reified T : Any> appendToQuestList(baseData: BaseQuest, questInfo: T) {
        val currentQuests = getQuestList().toMutableList()
        currentQuests.add(baseData)
        saveQuestList(currentQuests)
        sharedPreferences.edit()
            .putString("quest_data_${baseData.title}", json.encodeToString(questInfo))
            .apply()
    }


    inline fun <reified T : Any> getQuestInfo(baseData: BaseQuest): T? {
        val questInfo = sharedPreferences.getString("quest_data_${baseData.title}", null) ?: return null
        return try {
            json.decodeFromString<T>(questInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    inline fun <reified T : Any> updateQuestInfo(
        baseData: BaseQuest,
        updateAction: (T) -> T
    ) {
        val data = getQuestInfo<T>(baseData)
        if (data != null) {
            val updatedData = updateAction(data)
            sharedPreferences.edit()
                .putString("quest_data_${baseData.title}", json.encodeToString(updatedData))
                .apply()
        }
    }



    fun isQuestCompleted(title:String, date: String): Boolean? {
        val lastPerformed =
            sharedPreferences.getString(QUEST_LAST_PERFORMED_SUFFIX + title, null) ?: return null

        return lastPerformed == date
    }

    fun markQuestAsComplete(title:String, date: String, isComplete: Boolean) {
        sharedPreferences.edit().putString(QUEST_LAST_PERFORMED_SUFFIX + title, date).apply()
    }

    /**
     * Filters out all the quests that need to be performed today
     *
     * @param quests
     * @return
     */
    fun filterQuestForToday(quests: List<BaseQuest>): List<BaseQuest> {
        val today = getCurrentDay()
        Log.d("current day",today.name)
        Log.d("all quests ",quests.toString())
        return quests.filter { it.selectedDays.contains(today) }
    }

    companion object {
        private const val PREF_NAME = "all_quest_preferences"
        private const val ALL_QUESTS_LIST_KEY = "quest_list"
        private const val QUEST_LAST_PERFORMED_SUFFIX = "date_wen_quest_lst_d_"

    }
}