package launcher.launcher.data.game

import kotlinx.serialization.Serializable

@Serializable
enum class Achievements(val xp:Int, val message: String) {
    MONTH_STREAK(1000,""),
    WEEK_STREAK(25,""),
    THE_DISCIPLINED(25,""),
}