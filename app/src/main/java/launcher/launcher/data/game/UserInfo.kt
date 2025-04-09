package launcher.launcher.data.game

import android.content.Context
import kotlinx.serialization.Serializable
import launcher.launcher.utils.json
import androidx.core.content.edit
import launcher.launcher.utils.isTimeOver


/**
 * Represents the user's information in the game
 * @param activeBoosts A map of active boosts in the game. Format <BoostObject,Timestamp>
 *     timeStamp format: yyyy-dd-mm-hh-mm
 */
@Serializable
data class UserInfo(
    var xp : Int= 0,
    var level : Int = 1,
    val inventory: HashMap<Rewards, Int> = hashMapOf(Pair(Rewards.STREAK_FREEZER,2)),
    val achievements: List<Achievements> = listOf(Achievements.THE_DISCIPLINED,Achievements.MONTH_STREAK),
    var activeBoosts: HashMap<Rewards,String> = hashMapOf()
)


/**
 * Represents the user in the game
 * @param lastXpEarned The amount of xp that user earned the last time through means like streak, quests etc. Used by dialogs and stuff to display information
 * @param lastRewards same as [lastXpEarned] but instead for rewards
 */
object User {
    lateinit var appContext: Context
    lateinit var userInfo: UserInfo
    lateinit var streakData: StreakData
    var lastXpEarned: Int? = null
    var lastRewards: List<Rewards>? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        userInfo = getUserInfo(appContext)
        streakData = getStreakInfo(appContext)
    }
}


/**
 * Converts the level to xp required to level up
 */
fun xpToLevelUp(level: Int): Int {
    return (50 * level * level - 50 * level)
}

/**
 * The xp that is rewarded when user completes a quest
 */
fun xpToRewardForQuest(level: Int, multiplier: Int = 1): Int {
    return (20 * level + 30) * multiplier
}

/**
 * Adds xp to the user and checks if the user has leveled up
 * @param xp The xp to add
 * @return userinfo with the new xp and level
 */
fun User.addXp(xp: Int){
    removeInactiveBooster()
    val multiplier = if(isBoosterActive(Rewards.XP_BOOSTER)) 2 else 1
    userInfo.xp += xp * multiplier
    while(userInfo.xp >= xpToLevelUp(userInfo.level+1)){
        userInfo.level++
    }
    saveUserInfo()
}

fun User.removeInactiveBooster() {
    userInfo.activeBoosts.forEach {
        if(isTimeOver(it.value)){
            userInfo.activeBoosts.remove(it.key)
        }
    }
    saveUserInfo()
}

fun User.isBoosterActive(reward: Rewards): Boolean{
    return isTimeOver(userInfo.activeBoosts.getOrDefault(reward,""))
}

fun User.addItemsToInventory(items: HashMap<Rewards, Int>){
    items.forEach {
        userInfo.inventory.put(it.key,it.value+getInventoryItemCount(it.key))
    }
    saveUserInfo()
}


fun User.saveUserInfo(){
    val sharedPreferences = appContext.getSharedPreferences("user_info", Context.MODE_PRIVATE)
    sharedPreferences.edit { putString("user_info", json.encodeToString(userInfo)) }
}


fun User.getInventoryItemCount(item: Rewards): Int{
    return userInfo.inventory.getOrDefault(item,0)
}

fun User.useInventoryItem(item: Rewards,count:Int = 1){
    if(userInfo.inventory.getOrDefault(item,0) > 0){
        userInfo.inventory.put(item,getInventoryItemCount(item)-count)
        if(getInventoryItemCount(item) == 0){
            userInfo.inventory.remove(item)
        }
        saveUserInfo()
    }
}

fun getUserInfo(context: Context): UserInfo {
    val sharedPreferences = context.getSharedPreferences("user_info", Context.MODE_PRIVATE)
    val userInfoJson = sharedPreferences.getString("user_info", null)
    return if (userInfoJson != null) {
        json.decodeFromString(userInfoJson)
    } else {
        UserInfo()
    }
}