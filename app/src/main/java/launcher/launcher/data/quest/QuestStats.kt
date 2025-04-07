package launcher.launcher.data.quest

import launcher.launcher.ui.screens.launcher.components.getCurrentTime

data class QuestStats(
    val streak:Int = 0,
    val lastCompleted: String = getCurrentTime(),
)