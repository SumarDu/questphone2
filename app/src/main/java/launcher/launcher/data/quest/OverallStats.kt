package launcher.launcher.data.quest

import kotlinx.serialization.Serializable

@Serializable
data class OverallStats(
    val questsPerformed: Int,
    val totalQuests : Int
)

/**
 * Required because I was too lazy to figure out how to serialize local date in the [OverallStats] class
 */
data class OverallStatsUs(
    val date: kotlinx.datetime.LocalDate,
    val completedQuests: Int,
    val totalQuests: Int,
    val isPlaceholder: Boolean = false
)

/**
 * Stores stats for individual quests
 * @param completedTime Time when this quest was completed
 */
@Serializable
data class QuestStats(
    val isSuccessful: Boolean,
    val completedTime: String,
)