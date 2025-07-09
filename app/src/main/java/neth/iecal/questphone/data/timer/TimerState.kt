package neth.iecal.questphone.data.timer

import java.time.Duration

enum class TimerMode {
    INACTIVE,
    QUEST_COUNTDOWN,
    BREAK,
    OVERTIME
}

data class TimerState(
    val mode: TimerMode = TimerMode.INACTIVE,
    val time: Duration = Duration.ZERO,
    val activeQuestId: String? = null,
    val isBreakOvertime: Boolean = false
)
