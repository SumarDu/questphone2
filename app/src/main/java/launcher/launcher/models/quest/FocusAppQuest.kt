package launcher.launcher.models.quest

import kotlinx.serialization.Serializable

@Serializable
data class FocusAppQuestInfo(
    val questInfo: BaseQuestInfo,
    val initialTime: Int,
    val incrementValue: Int,
    val goalTime:Int
)