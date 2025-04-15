package launcher.launcher.data.quest

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class QuestStats(
    val questsPerformed: Int,
    val totalQuests : Int
)

/**
 * Required because I was too lazy to figure out how to serialize local date in the [QuestStats] class
 */
data class QuestStatUS(
    val date: LocalDate,
    val completedQuests: Int,
    val totalQuests: Int
)
