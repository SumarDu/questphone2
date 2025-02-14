package launcher.launcher.models.quest

import kotlinx.serialization.Serializable

@Serializable
data class BaseQuestInfo(
    val title: String = "",
    val instructions: List<String> = emptyList(),
    val reward: Int = 5,
)