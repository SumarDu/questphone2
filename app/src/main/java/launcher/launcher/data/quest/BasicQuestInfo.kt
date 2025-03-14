package launcher.launcher.data.quest

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import launcher.launcher.data.DayOfWeek
import launcher.launcher.data.IntegrationId
import launcher.launcher.ui.screens.quest.setup.ExcludeFromReviewDialog

/**
 * Stores basic information about a quests
 *
 * @property title this should be unique as it also acts as a primary key
 * @property instructions a list of instructions
 * @property reward
 * @property integrationId
 * @property selectedDays
 * @property currentQuestState
 */
@Serializable
data class BasicQuestInfo(
    var title: String = "",
    var instructions: String = "",
    val reward: Int = 5,
    var integrationId : IntegrationId = IntegrationId.DEEP_FOCUS,
    var selectedDays: Set<DayOfWeek> = emptySet(),
)


@Stable
class BaseQuestState(
    initialTitle: String = "",
    initialInstructions: String = "",
    initialReward: Int = 5,
    initialIntegrationId: IntegrationId = IntegrationId.DEEP_FOCUS,
    initialSelectedDays: Set<DayOfWeek> = emptySet()
) {
    var title by mutableStateOf(initialTitle)
    var instructions by mutableStateOf(initialInstructions)
    var reward by mutableIntStateOf(initialReward)
    var integrationId by mutableStateOf(initialIntegrationId)
    var selectedDays by mutableStateOf(initialSelectedDays)

    fun toBaseQuest() = BasicQuestInfo(title, instructions, reward, integrationId, selectedDays)
}
