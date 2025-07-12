package neth.iecal.questphone.data.timer

import java.time.Duration

enum class TimerMode {
    INACTIVE,
    QUEST_COUNTDOWN,
    BREAK,
    OVERTIME,
    UNPLANNED_BREAK,
    INFO
}

data class TimerState(
    val mode: TimerMode = TimerMode.INACTIVE,
    val time: Duration = Duration.ZERO,
    val activeQuestId: String? = null,
    val isBreakOvertime: Boolean = false,
    val questEndsAt: Long = 0,
    val isDeepFocusLocking: Boolean = false
)
