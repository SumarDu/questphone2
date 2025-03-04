package launcher.launcher.data.quest

import kotlinx.serialization.Serializable

/**
 * The Deep Focus Quest blocks all apps except a few selected ones.
 *
 * @property questInfo
 * @property focusTimeConfig
 * @property unrestrictedApps list of unrestricted apps during the focus session
 */

@Serializable
data class DeepFocus(
    val questInfo: BaseQuest = BaseQuest(),
    val focusTimeConfig: FocusTimeConfig = FocusTimeConfig(),
    val unrestrictedApps: Set<String> = emptySet(),
    var nextFocusDuration : Long = 300_000
)