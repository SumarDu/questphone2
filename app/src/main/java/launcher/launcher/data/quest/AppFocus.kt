package launcher.launcher.data.quest

import kotlinx.serialization.Serializable

/**
 * Block all apps except a specific selected app
 *
 * @property questInfo
 * @property focusQuestInfo
 * @property selectedApp app that user wants to concentrate on
 */
@Serializable
data class AppFocus(
    val questInfo: BaseQuest = BaseQuest(),
    val focusQuestInfo: FocusTimeConfig = FocusTimeConfig(),
    val selectedApp: String = "",
    val nextFocusDuration : Long = 300_000
)