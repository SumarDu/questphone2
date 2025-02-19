package launcher.launcher.models.quest

import kotlinx.serialization.Serializable

/**
 * The Focus quest blocks all apps except a few ones.
 *
 * @property questInfo
 * @property focusTimeConfig
 * @property selectedApps list of unrestricted apps during the focus session
 */

@Serializable
data class FocusQuestInfo(
    val questInfo: BaseQuestInfo,
    val focusTimeConfig: FocusTimeConfig,
    val selectedApps: Set<String>
)