package launcher.launcher.data.quest

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import launcher.launcher.data.DayOfWeek
import launcher.launcher.data.IntegrationId
import launcher.launcher.utils.getCurrentDate

/**
 * Stores basic information about a quests
 *
 * @property title this should be unique as it also acts as a primary key
 * @property reward
 * @property integrationId
 * @property selectedDays
 * @property autoDestruct format yyyy-mm-dd
 * @property timeRange format startHour,endHour
 */
@Serializable
data class BasicQuestInfo(
    var title: String = "",
    val reward: Int = 5,
    var integrationId : IntegrationId = IntegrationId.DEEP_FOCUS,
    var selectedDays: Set<DayOfWeek> = emptySet(),
    var autoDestruct: String = "9999-12-31",
    var timeRange: List<Int> = listOf(0,24),
    var createdOn : String = getCurrentDate(),
)


@Stable
class BaseQuestState(
    initialTitle: String = "",
    initialInstructions: String = "",
    initialReward: Int = 5,
    initialIntegrationId: IntegrationId = IntegrationId.DEEP_FOCUS,
    initialSelectedDays: Set<DayOfWeek> = emptySet(),
    initialAutoDestruct: String = "9999-12-31",
    initialTimeRange: List<Int> = listOf(0,24),
) {
    var title by mutableStateOf(initialTitle)
    var reward by mutableIntStateOf(initialReward)
    var integrationId by mutableStateOf(initialIntegrationId)
    var selectedDays by mutableStateOf(initialSelectedDays)
    var instructions by mutableStateOf(initialInstructions)
    var initialAutoDestruct by mutableStateOf(initialAutoDestruct)
    var initialTimeRange by mutableStateOf(initialTimeRange)
    fun toBaseQuest() = BasicQuestInfo(
        title = title,
        reward = reward,
        integrationId = integrationId,
        selectedDays = selectedDays,
        autoDestruct = initialAutoDestruct,
        timeRange = initialTimeRange,
    )
}
