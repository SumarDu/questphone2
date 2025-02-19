package launcher.launcher.models.quest

import kotlinx.serialization.Serializable

/**
 * Block all apps except a specific selected app
 *
 * @property questInfo
 * @property focusQuestInfo
 * @property selectedApp app that user wants to concentrate on
 */
@Serializable
data class FocusAppQuestInfo(
    val questInfo: BaseQuestInfo = BaseQuestInfo(),
    val focusQuestInfo: FocusTimeConfig,
    val selectedApp: String
)