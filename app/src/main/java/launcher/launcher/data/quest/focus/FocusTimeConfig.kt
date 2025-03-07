package launcher.launcher.data.quest.focus

import kotlinx.serialization.Serializable
import launcher.launcher.ui.screens.quest.setup.ExcludeFromReview

/**
 * Stores time related data for quests
 *
 * @property initialTime
 * @property finalTime
 * @property incrementTime
 * @property initialUnit
 * @property finalUnit
 * @property incrementUnit
 */
@Serializable
data class FocusTimeConfig(
    var initialTime: String = "5",
    var finalTime: String = "5",
    var incrementTime: String = "15",
    var initialUnit: String = "m",
    var finalUnit: String = "h",
    var incrementUnit: String = "m"
) {

    @ExcludeFromReview
    val initialTimeInMs: Long get() = convertToMillis(initialTime, initialUnit)
    @ExcludeFromReview
    val finalTimeInMs: Long get() = convertToMillis(finalTime, finalUnit)
    @ExcludeFromReview
    val incrementTimeInMs: Long get() = convertToMillis(incrementTime, incrementUnit)

    private fun convertToMillis(time: String, unit: String): Long {
        val timeValue = time.toLongOrNull() ?: return 0L
        return when (unit) {
            "h" -> timeValue * 60 * 60 * 1000
            "m" -> timeValue * 60 * 1000
            else -> 0L
        }
    }
}
