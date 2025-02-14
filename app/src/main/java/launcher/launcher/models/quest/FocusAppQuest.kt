package launcher.launcher.models.quest

import kotlinx.serialization.Serializable

@Serializable
data class FocusAppQuestInfo(
    val questInfo: BaseQuestInfo = BaseQuestInfo(),
    val initialTime: Int = 10,
    val incrementValue: Int = 2,
    val goalTime:Int = 100
)