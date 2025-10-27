package neth.iecal.questphone.data.quest.focus

import kotlinx.serialization.Serializable
import neth.iecal.questphone.ui.screens.quest.setup.ExcludeFromReviewDialog


/**
 * The Deep Focus Quest blocks all apps except a few selected ones.
 *
 * @property focusTimeConfig
 * @property unrestrictedApps list of unrestricted apps during the focus session
 * @property nextFocusDurationInMillis
 */
@Serializable
data class DeepFocus(
    var focusTimeConfig: FocusTimeConfig = FocusTimeConfig(),
    val unrestrictedApps: List<String> = emptyList(),
    val currentRegularTopic: String? = null,
    val currentExtraTopic: String? = null,
    @ExcludeFromReviewDialog
    var nextFocusDurationInMillis : Long = 0L,
    var breakDurationInMillis: Long = 0L,
    var minWorkSessions: Int = 1,
    var maxWorkSessions: Int = 5,
    var longBreakDurationInMillis: Long = 0L,
    var reward_per_extra_session: Int = 0,
    // Optional random range for extra session coin reward
    var reward_extra_min: Int = 0,
    var reward_extra_max: Int = 0,
    // Diamonds: separate rewards for regular and extra sessions
    var diamond_reward_regular: Int = 0,
    var diamond_reward_extra: Int = 0,
    var long_break_after_sessions: Int = 0,
    @ExcludeFromReviewDialog
    var completedWorkSessions: Int = 0
){
    fun incrementTime() {
    if (nextFocusDurationInMillis < focusTimeConfig.finalTimeInMs) {
        nextFocusDurationInMillis = minOf(
            nextFocusDurationInMillis + focusTimeConfig.incrementTimeInMs,
            focusTimeConfig.finalTimeInMs
        )
    }
}
}

