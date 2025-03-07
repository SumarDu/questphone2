package launcher.launcher.data.quest.focus

import kotlinx.serialization.Serializable
import launcher.launcher.ui.screens.quest.setup.ExcludeFromReview


/**

 *
 * @property focusTimeConfig
 * @property selectedFocusApp
 * @property nextFocusDuration
 */
@Serializable
data class AppFocus(
    var focusTimeConfig: FocusTimeConfig = FocusTimeConfig(),
    var selectedFocusApp: String = "",
    @ExcludeFromReview
    var nextFocusDuration : Long = 0L
)

