package launcher.launcher.data.quest

import kotlinx.serialization.Serializable
import launcher.launcher.Constants
import launcher.launcher.data.DayOfWeek
import launcher.launcher.data.IntegrationId

/**
 * Stores basic information about a quests
 *
 * @property title this should be unique as it also acts as a primary key
 * @property instructions a list of instructions
 * @property reward
 * @property integrationId
 * @property selectedDays
 */
@Serializable
data class BaseQuest(
    val title: String = "",
    val instructions: List<String> = emptyList(),
    val reward: Int = 5,
    var integrationId : IntegrationId = IntegrationId.APP_FOCUS,
    val selectedDays: Set<DayOfWeek> = emptySet()
)