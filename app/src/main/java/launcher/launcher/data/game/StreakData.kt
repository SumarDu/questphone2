package launcher.launcher.data.game

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import launcher.launcher.utils.getCurrentDate
import launcher.launcher.utils.json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable
data class StreakData(
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var lastCompletedDate: String = "0001-01-01",
)

/**
 * Represents the return value of [checkIfStreakFailed]
 * @param streakFreezersUsed The number of streak freezers used if the user failed to complete the streak or null if not used
 */
data class StreakCheckReturn(
    val streakData: StreakData,
    val userInfo: UserInfo,
    val streakFreezersUsed: Int? = null,
    val isFailed: Boolean
)
/**
 * Checks if the user has failed a streak
 * @return returns null if the user has not failed the streak or [StreakCheckReturn] if user either failed to complete the streak or used streak freezers.
 */
fun checkIfStreakFailed(streakData: StreakData, userInfo: UserInfo, context: Context): StreakCheckReturn? {
    val today = LocalDate.now()
    val lastCompleted = LocalDate.parse(streakData.lastCompletedDate)
    val daysSince = ChronoUnit.DAYS.between(lastCompleted, today)
    Log.d("streak day since",daysSince.toString())

    // failed to complete streak
    if(daysSince>=2){
        // check if user has available streak freezers
        val requiredStreakFreezers = (daysSince-1).toInt()
        // user has enough streak freezers, hence use them
            if(getInventoryItemCount(userInfo,Rewards.STREAK_FREEZER) >= requiredStreakFreezers){
                val userInfo = useInventoryItem(userInfo,Rewards.STREAK_FREEZER,requiredStreakFreezers)
                saveUserInfo(userInfo,context)

                streakData.currentStreak += requiredStreakFreezers
                streakData.lastCompletedDate = today.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                saveStreakInfo(streakData,context)
                return StreakCheckReturn(
                    streakData,userInfo,requiredStreakFreezers,false
                )
            }else{
                // user has no streak freezer
                streakData.longestStreak = maxOf(streakData.currentStreak, streakData.longestStreak)
                streakData.currentStreak = 0
                saveStreakInfo(streakData,context)
                return StreakCheckReturn(
                    streakData,userInfo,null,true
                )
            }
    } else {
        return null
    }
}

fun continueStreak(streakData: StreakData,context: Context): StreakData? {
    val today = LocalDate.now()
    val lastCompleted = LocalDate.parse(streakData.lastCompletedDate)
    val daysSince = ChronoUnit.DAYS.between(lastCompleted, today)

    if(daysSince!=0L){
        streakData.currentStreak += 1
        streakData.longestStreak = maxOf(streakData.currentStreak, streakData.longestStreak)
        streakData.lastCompletedDate = getCurrentDate()
        saveStreakInfo(streakData,context)
        return streakData
    }
    return null
}

/**
 * Updates the streak information based on the current date and the last completed date.
 * @return format<StreakData,Int>,
 * possible returned int values:
 * -1: user lost his streak
 * -2: use continued streak
 * 0: No change
 * Any other number: Number of streak freezers used
 */
//fun updateStreak(streakData: StreakData, isDoneAll: Boolean = false,context: Context): Pair<StreakData, Int> {
//    val today = LocalDate.now()
//    val lastCompleted = LocalDate.parse(streakData.lastCompletedDate)
//
//    val daysSince = ChronoUnit.DAYS.between(lastCompleted, today)
//
//    Log.d("Days Since",daysSince.toString())
//    return when {
//            (daysSince == 1L && isDoneAll ) -> {
//                val newStreak = streakData.currentStreak + 1
//                Pair(streakData.copy(
//                    currentStreak = newStreak,
//                    longestStreak = maxOf(newStreak, streakData.longestStreak),
//                    lastCompletedDate = today.toString(),
//                ),-2)
//            }
//
//        daysSince >= 2 -> {
//            var userInfo = getUserInfo(context)
//            val requiredStreakFreezers = (daysSince-2).toInt()
//            if(getInventoryItemCount(userInfo,Rewards.STREAK_FREEZER) >= requiredStreakFreezers){
//                userInfo = useInventoryItem(userInfo,Rewards.STREAK_FREEZER,requiredStreakFreezers)
//                Pair(streakData,requiredStreakFreezers)
//            }else{
//                Pair(streakData.copy(
//                    currentStreak = 0,
//                    longestStreak = maxOf(0, streakData.longestStreak),
//                ),-1)
//            }
//        }
//
//        else -> {
//            // Nothing changes
//            Pair(streakData,0)
//        }
//    }
//}


fun saveStreakInfo(streak: StreakData, context: Context){
    val sharedPreferences = context.getSharedPreferences("overall_streak", Context.MODE_PRIVATE)
    Log.d("Saved streak",streak.toString())
    sharedPreferences.edit(commit = true) { putString("info", json.encodeToString(streak)) }
    Log.d("encoded", json.encodeToString<StreakData>(streak))
}

fun getStreakInfo(context: Context): StreakData {
    val sharedPreferences = context.getSharedPreferences("overall_streak", Context.MODE_PRIVATE)
    val streakInfoJson = sharedPreferences.getString("info", null)
    Log.d("streak json", streakInfoJson.toString())
    if (streakInfoJson != null) {
        return json.decodeFromString<StreakData>(streakInfoJson)
    } else {
        val x = StreakData()
        saveStreakInfo(x,context)
        return x
    }
}

fun xpFromStreak(dayStreak: Int): Int {
    return (10 * dayStreak) + (dayStreak * dayStreak / 2)
}
