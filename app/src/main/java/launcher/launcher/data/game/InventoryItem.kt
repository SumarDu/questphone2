package launcher.launcher.data.game

import kotlinx.serialization.Serializable
import launcher.launcher.utils.getFullTimeAfter
import launcher.launcher.R

@Serializable
enum class Availability(val displayName: String, val rarityValue: Int) {
    COMMON("Common", 1),
    UNCOMMON("Uncommon", 2),
    RARE("Rare", 3),
    EPIC("Epic", 4),
    LEGENDARY("Legendary", 5),
    LIMITED_TIME("Limited Time", 6)
}

@Serializable
enum class Category(val simpleName: String){
    BOOSTERS("Boosters"),
    CHESTS("Chests"),
    TOOLS("Tools"),
    CUSTOMIZATION("Customization")
}

@Serializable
enum class InventoryItem(val simpleName: String, val description: String, val icon: Int, val isUsableFromInventory : Boolean = false, val onUse: () -> Unit = {}, val availability: Availability = Availability.UNCOMMON, val price: Int = 0, val category: Category = Category.TOOLS) {
    STREAK_FREEZER("Streak Freezer", description = "Automatically freezes your streak in case you fail to complete all quests on a day", icon = R.drawable.streak_freezer),
    QUEST_SKIPPER("Quest Skipper", description = "This item can be used to mark a quest as complete if you fail to do it in time (must be used the same day of failure) or skip it in case you feel like not performing one.", icon = R.drawable.quest_skipper),

    XP_BOOSTER ("XP Booster", description = "Get 2x more xp for the next 5 hours.", isUsableFromInventory = true,onUse = ::onUseXpBooster, icon = R.drawable.xp_booster, category = Category.BOOSTERS)
}

fun onUseXpBooster(){
    User.userInfo.active_boosts.put(InventoryItem.XP_BOOSTER,getFullTimeAfter(5,0))
    User.saveUserInfo()
}