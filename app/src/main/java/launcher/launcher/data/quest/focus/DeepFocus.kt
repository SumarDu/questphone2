package launcher.launcher.data.quest.focus

import kotlinx.serialization.Serializable
import launcher.launcher.ui.screens.quest.setup.ExcludeFromReview


/**
 * The Deep Focus Quest blocks all apps except a few selected ones.
 *
 * @property focusTimeConfig
 * @property unrestrictedApps list of unrestricted apps during the focus session
 * @property nextFocusDuration
 */
@Serializable
data class DeepFocus(
    var focusTimeConfig: FocusTimeConfig = FocusTimeConfig(),
    var unrestrictedApps: Set<String> = emptySet(),
    @ExcludeFromReview
    var nextFocusDuration : Long = 0L
)

