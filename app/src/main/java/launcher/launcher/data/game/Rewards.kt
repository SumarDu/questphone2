package launcher.launcher.data.game

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import launcher.launcher.utils.getFullTimeAfter

@Serializable
enum class Rewards(val simpleName: String, val description: String, val isUsableFromInventory : Boolean = false, val onUse: () -> Unit = {}) {
    XP_BOOSTER ("XP Booster", description = "Get 2x more xp for the next 5 hours.", isUsableFromInventory = true,onUse = ::onUseXpBooster),
    STREAK_FREEZER("Streak Freezer", description = "Automatically freezes your streak in case you fail to complete all quests on a day" ),
    QUEST_SKIPPER("Quest Skipper", description = "This item can be used to mark a quest as complete if you fail to do it in time (must be used the same day of failure) or skip it in case you feel like not performing one.")
}

fun onUseXpBooster(){
    User.userInfo.activeBoosts.put(Rewards.XP_BOOSTER,getFullTimeAfter(5,0))
    User.saveUserInfo()
}
