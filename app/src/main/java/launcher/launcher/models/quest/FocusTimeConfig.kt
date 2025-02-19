package launcher.launcher.models.quest

import kotlinx.serialization.Serializable

/**
 * Stores time related data for quests
 *
 * @property initialTime
 * @property finalTime
 * @property incrementTime
 * @property initialUnit
 * @property finalUnit
 * @property incrementUnit
 */
@Serializable
data class FocusTimeConfig(
    var initialTime: String = "1",
    var finalTime: String = "5",
    var incrementTime: String = "15",
    var initialUnit: String = "h",
    var finalUnit: String = "h",
    var incrementUnit: String = "m"
)