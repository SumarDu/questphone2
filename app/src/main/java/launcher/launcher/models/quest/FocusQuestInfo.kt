package launcher.launcher.models.quest

import kotlinx.serialization.Serializable

/**
 * The Focus quest blocks all apps except a few ones.
 *
 * @property questInfo
 * @property focusTimeConfig
 * @property unrestrictedApps list of unrestricted apps during the focus session
 */

@Serializable
data class FocusQuestInfo(
    val questInfo: BaseQuestInfo = BaseQuestInfo(),
    val focusTimeConfig: FocusTimeConfig = FocusTimeConfig(),
    val unrestrictedApps: Set<String> = emptySet(),
    var nextFocusDuration : Long = 300_000
)