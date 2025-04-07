package launcher.launcher.data.game

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.serialization.Serializable
import launcher.launcher.utils.json
import androidx.core.content.edit

/**
 * Represents the user's information in the game
 */
@Serializable
data class UserInfo(
    var xp : Int= 0,
    var level : Int = 1,
    val inventory: HashMap<Rewards, Int> = hashMapOf(Pair(Rewards.STREAK_FREEZER,2)),
    val achievements: List<Achievements> = listOf(Achievements.THE_DISCIPLINED,Achievements.MONTH_STREAK),
    val streak: Int = 1,
    var coins : Int = 10,
    var activeBoosts: ActiveBoosts = ActiveBoosts()
)

/**
 * Represents the active boosts in the game
 * @param xpBooster The xp booster that is currently active. Format <Booster Type, End Minute>
 */
@Serializable
data class ActiveBoosts(
    val xpBooster: HashMap<Int,String> = hashMapOf()
)

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
 * @param user The user to add xp to
 * @param xp The xp to add
 * @return userinfo with the new xp and level
 */
fun addXP(user: UserInfo, xp: Int): UserInfo {
    val multiplier = if(user.activeBoosts.xpBooster.isNotEmpty()) 2 else 1
    user.xp += xp * multiplier

    while(user.xp >= xpToLevelUp(user.level+1)){
        user.level++
    }
    return user
}

fun saveUserInfo(user: UserInfo, context: Context){
    val sharedPreferences = context.getSharedPreferences("user_info", Context.MODE_PRIVATE)
    sharedPreferences.edit { putString("user_info", json.encodeToString(user)) }
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

fun getInventoryItemCount(user: UserInfo,item: Rewards): Int{
    return user.inventory.getOrDefault(item,0)
}

fun useInventoryItem(user: UserInfo,item: Rewards): UserInfo{
    if(user.inventory.getOrDefault(item,0) > 0){
        user.inventory.put(item,user.inventory.getOrDefault(item,0)-1)
        if(user.inventory.getOrDefault(item,0) == 0){
            user.inventory.remove(item)
        }
    }
    return user
}