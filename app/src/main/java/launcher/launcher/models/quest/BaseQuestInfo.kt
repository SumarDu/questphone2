package launcher.launcher.models.quest

import kotlinx.serialization.Serializable
import launcher.launcher.Constants

@Serializable
data class BaseQuestInfo(
    val title: String = "",
    val instructions: List<String> = emptyList(),
    val reward: Int = 5,
    var integrationId : Int = Constants.INTEGRATION_ID_FOCUS
)