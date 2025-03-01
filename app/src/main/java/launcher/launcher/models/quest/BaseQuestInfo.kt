package launcher.launcher.models.quest

import kotlinx.serialization.Serializable
import launcher.launcher.Constants
import launcher.launcher.models.DayOfWeek

/**
 * Stores basic information quests
 *
 * @property title this should be unique as it also acts as a key
 * @property instructions a list of instructions
 * @property reward
 * @property integrationId
 * @property selectedDays
 */
@Serializable
data class BaseQuestInfo(
    val title: String = "",
    val instructions: List<String> = emptyList(),
    val reward: Int = 5,
    var integrationId : Int = Constants.INTEGRATION_ID_FOCUS,
    val selectedDays: Set<DayOfWeek> = emptySet()
)