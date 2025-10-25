package neth.iecal.questphone.data.game

import android.content.Context
import androidx.core.content.edit
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import neth.iecal.questphone.data.game.User.lastRewards
import neth.iecal.questphone.data.game.User.lastXpEarned
import neth.iecal.questphone.utils.formatInstantToDate
import neth.iecal.questphone.utils.isTimeOver
import neth.iecal.questphone.utils.json
import neth.iecal.questphone.utils.triggerProfileSync
import java.util.UUID


/**
 * Represents the user's information in the game
 * @param active_boosts A map of active boosts in the game. Format <BoostObject,Timestamp>
 *     timeStamp format: yyyy-dd-mm-hh-mm
 */
@Serializable
data class UserInfo(
    var id : String = UUID.randomUUID().toString(),
    var username: String = "",
    var full_name: String = "",
    var has_profile: Boolean = false,
    var xp : Int= 0,
    var coins: Int = 0,
    // Diamonds available to spend
    var diamonds: Int = 0,
    // Diamonds earned today (pending; not spendable until next day)
    var diamonds_pending: Int = 0,
    // Tokens earned from completing quests (quest_title -> count)
    val tokens: MutableMap<String, Int> = mutableMapOf(),

    var level : Int = 1,
    val inventory: HashMap<InventoryItem, Int> = hashMapOf(Pair(InventoryItem.STREAK_FREEZER,2)),
    val achievements: List<Achievements> = listOf(Achievements.THE_EARLY_FEW),
    var active_boosts: HashMap<InventoryItem,String> = hashMapOf(),
    var last_updated: Long = System.currentTimeMillis(),
    var created_on: Instant = Clock.System.now(),
    var streak : StreakData = StreakData(),
    @Transient
    var needsSync: Boolean = true,
    @Transient
    var isAnonymous : Boolean = false,
){
    fun getFirstName(): String {
        return full_name.trim().split(" ").firstOrNull() ?: ""
    }

    fun getCreatedOnString():String{
        return formatInstantToDate(created_on)
    }
}


/**
 * Represents the user in the game
 * @param lastXpEarned The amount of xp that user earned the last time through means like streak, quests etc. Used by dialogs and stuff to display information
 * @param lastRewards same as [lastXpEarned] but instead for rewards
 */
object User {
    lateinit var appContext: Context
    lateinit var userInfo: UserInfo

    var lastXpEarned: Int? = null
    var lastRewards: List<InventoryItem>? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        userInfo = getUserInfo(appContext)
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
    val multiplier = if(isBoosterActive(InventoryItem.XP_BOOSTER)) 2 else 1
    userInfo.xp += xp * multiplier
    while(userInfo.xp >= xpToLevelUp(userInfo.level+1)){
        userInfo.level++
    }
    saveUserInfo()
}

fun User.removeInactiveBooster() {
    userInfo.active_boosts.forEach {
        if(isTimeOver(it.value)){
            userInfo.active_boosts.remove(it.key)
        }
    }
    saveUserInfo()
}

fun User.isBoosterActive(reward: InventoryItem): Boolean {
    if (userInfo.active_boosts.contains(reward)) {
        val isBoosterActive =
            !isTimeOver(userInfo.active_boosts.getOrDefault(reward, "9999-09-09-09-09"))
        if (isBoosterActive) removeInactiveBooster()
        return isBoosterActive
    }
    return false

}

fun User.addItemsToInventory(items: HashMap<InventoryItem, Int>){
    items.forEach {
        userInfo.inventory.put(it.key,it.value+getInventoryItemCount(it.key))
    }
    saveUserInfo()
}


fun User.saveUserInfo(isSetLastUpdated: Boolean = true){
    val sharedPreferences = appContext.getSharedPreferences("user_info", Context.MODE_PRIVATE)
    if(isSetLastUpdated && !User.userInfo.isAnonymous){
        userInfo.last_updated = System.currentTimeMillis()
        userInfo.needsSync = true
        triggerProfileSync(appContext)
    }
    sharedPreferences.edit { putString("user_info", json.encodeToString(userInfo)) }
}


fun User.getInventoryItemCount(item: InventoryItem): Int{
    return userInfo.inventory.getOrDefault(item,0)
}

fun User.useInventoryItem(item: InventoryItem, count:Int = 1){
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
fun User.useCoins(coins: Int){
    userInfo.coins-=coins
    saveUserInfo()
}
fun User.addCoins(coins:Int){
    userInfo.coins+=coins
    saveUserInfo()
}

// Diamonds management
// Add newly earned diamonds to pending so they are not spendable until next day
fun User.addDiamonds(amount: Int){
    if (amount <= 0) return
    userInfo.diamonds_pending += amount
    saveUserInfo()
}

fun User.useDiamonds(amount: Int): Boolean {
    if (amount <= 0) return false
    if (userInfo.diamonds < amount) return false
    userInfo.diamonds -= amount
    saveUserInfo()
    return true
}

// Token management - each token represents a completed quest with its title
fun User.addToken(questTitle: String) {
    val currentCount = userInfo.tokens[questTitle] ?: 0
    userInfo.tokens[questTitle] = currentCount + 1
    saveUserInfo()
}

fun User.getTotalTokens(): Int {
    return userInfo.tokens.values.sum()
}

fun User.getTokens(): Map<String, Int> {
    return userInfo.tokens.toMap()
}