package neth.iecal.questphone.data.game

import android.util.Log
import kotlinx.serialization.Serializable
import neth.iecal.questphone.R
import neth.iecal.questphone.ui.navigation.Navigator
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.screens.onboard.SelectAppsModes
import neth.iecal.questphone.utils.getFullTimeAfter

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
    UTILITY("Utility"),
    UNLOCKERS("Unlockers"),
}

@Serializable
enum class InventoryItem(val simpleName: String, val description: String, val icon: Int, val isDirectlyUsableFromInventory : Boolean = false, val onUse: () -> Unit = {}, val availability: Availability = Availability.UNCOMMON, val price: Int = 0, val category: Category = Category.UTILITY) {
    STREAK_FREEZER("Streak Freezer", description = "Automatically freezes your streak in case you fail to complete all quests on a day", icon = R.drawable.streak_freezer, category = Category.BOOSTERS, price = 20),

    XP_BOOSTER ("XP Booster", description = "Get 2x more xp for the next 5 hours.", isDirectlyUsableFromInventory = true,onUse = ::onUseXpBooster, icon = R.drawable.xp_booster, category = Category.BOOSTERS, price = 10),
    DISTRACTION_ADDER("Distraction Adder", description = "Add an app to the distraction list", isDirectlyUsableFromInventory = true, onUse = ::onUseDistractionAdder,icon = R.drawable.distraction_adder, price = 2),
    DISTRACTION_REMOVER("Distraction Remover", description = "Remove an app from the distractions list", isDirectlyUsableFromInventory = true, onUse = ::onUseDistractionRemover ,icon = R.drawable.distraction_remover, price = 20),
}

fun onUseXpBooster(){
    User.userInfo.active_boosts.put(InventoryItem.XP_BOOSTER, getFullTimeAfter(5, 0))
    User.saveUserInfo()
}

fun onUseDistractionAdder(){
    Log.d("InventoryItem","Used distraction Adder")
    Navigator.currentScreen = Screen.SelectApps.route + SelectAppsModes.ALLOW_ADD.ordinal
}
fun onUseDistractionRemover(){
    Log.d("InventoryItem","Used distraction Remover")
    Navigator.currentScreen = Screen.SelectApps.route + SelectAppsModes.ALLOW_REMOVE.ordinal
}