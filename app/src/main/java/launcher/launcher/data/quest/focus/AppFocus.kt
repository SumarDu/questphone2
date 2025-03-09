package launcher.launcher.data.quest.focus

import kotlinx.serialization.Serializable
import launcher.launcher.ui.screens.quest.setup.ExcludeFromReviewDialog


/**
 *
 * @property focusTimeConfig
 * @property selectedFocusApp
 * @property nextFocusDurationInMillis
 */
@Serializable
data class AppFocus(
    var focusTimeConfig: FocusTimeConfig = FocusTimeConfig(),
    var selectedFocusApp: String = "",
    @ExcludeFromReviewDialog
    var nextFocusDurationInMillis : Long = 0L
)

