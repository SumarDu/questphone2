package launcher.launcher.data.quest

import kotlinx.serialization.Serializable

@Serializable
data class QuestStats(
    val day: String,
    val questsPerformed: Int,
    val totalQuests : Int
)