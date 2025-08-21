package neth.iecal.questphone.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.QuestPriority
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import neth.iecal.questphone.ui.screens.launcher.QuestTile
import neth.iecal.questphone.ui.screens.launcher.viewQuest
import neth.iecal.questphone.utils.SchedulingUtils
import neth.iecal.questphone.utils.isAllDayRange
import neth.iecal.questphone.utils.formatTimeMinutes
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.toMinutesRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private fun formatDuration(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return buildString {
        if (h > 0) append("${h}h")
        if (m > 0) {
            if (isNotEmpty()) append(" ")
            append("${m}m")
        }
        if (isEmpty()) append("0m")
    }
}

@Composable
fun DayQuestsScreen(navController: NavController, key: String) {
    val context = LocalContext.current
    val dao = QuestDatabaseProvider.getInstance(context).questDao()
    val allQuests by dao.getAllQuests().collectAsState(initial = emptyList())
    val timerState by TimerService.timerState.collectAsState()

    val dates: List<LocalDate> = when (key.uppercase()) {
        "YDAY" -> listOf(LocalDate.now().minusDays(1))
        "TODAY" -> listOf(LocalDate.now())
        "TMRW" -> listOf(LocalDate.now().plusDays(1))
        "WKEND" -> {
            val today = LocalDate.now()
            val saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            val sunday = saturday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            listOf(saturday, sunday)
        }
        else -> listOf(LocalDate.now())
    }

    val planned = remember(allQuests, dates) {
        allQuests.filter { q ->
            !q.is_destroyed && dates.any { d -> SchedulingUtils.isQuestAvailableOnDate(q.scheduling_info, d) }
        }
    }

    val completedTitles = remember { mutableStateListOf<String>() }
    completedTitles.clear()
    completedTitles.addAll(planned.filter { it.last_completed_on == getCurrentDate() }.map { it.title })

    val tileHeight = 72.dp
    val tileSpacing = 12.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(tileSpacing)
    ) {
        Text(
            text = when (key.uppercase()) {
                "YDAY" -> "Yesterday"
                "TODAY" -> "Today"
                "TMRW" -> "Tomorrow"
                "WKEND" -> "Weekend"
                else -> key
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val sortedQuests = planned.sortedWith(
            compareBy(
                { if (isAllDayRange(it.time_range)) 1 else 0 },
                { toMinutesRange(it.time_range).first }
            )
        )

        sortedQuests.forEach { baseQuest ->
            val (startMin, endMin) = toMinutesRange(baseQuest.time_range)

            val isCompleted = completedTitles.contains(baseQuest.title)
            val isActive = timerState.activeQuestId == baseQuest.id && (timerState.mode == TimerMode.QUEST_COUNTDOWN || timerState.mode == TimerMode.BREAK)

            val statusColor = when (baseQuest.priority) {
                QuestPriority.IMPORTANT_URGENT -> Color(0xFFEF4444)
                QuestPriority.IMPORTANT_NOT_URGENT -> Color(0xFF10B981)
                QuestPriority.NOT_IMPORTANT_URGENT -> Color(0xFFF5DEB3)
                QuestPriority.STABLE -> Color(0xFF3B82F6)
                QuestPriority.NOT_IMPORTANT_NOT_URGENT -> Color(0xFFD1D5DB)
            }

            val enabled = true

            val durationText = formatDuration(baseQuest.quest_duration_minutes)
            val startText = if (isAllDayRange(baseQuest.time_range)) null else formatTimeMinutes(startMin)
            val endText = if (isAllDayRange(baseQuest.time_range)) null else formatTimeMinutes(endMin)
            val deadlineTextOnly = if (baseQuest.deadline_minutes >= 0) formatTimeMinutes(baseQuest.deadline_minutes) else null

            val containerColor = Color(0xFF1F2937)

            // Subtasks progress
            val instructionsText = baseQuest.instructions ?: ""
            val lines = instructionsText.split("\n")
            var totalSubtasks = 0
            var doneSubtasks = 0

            val sp = context.getSharedPreferences("quest_checkboxes", MODE_PRIVATE)
            val savedStatesStr = sp.getString(baseQuest.id, "") ?: ""
            val savedMap = mutableMapOf<String, Boolean>()
            if (savedStatesStr.isNotEmpty()) {
                savedStatesStr.split(",").forEach { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        savedMap[parts[0]] = parts[1].toBoolean()
                    }
                }
            }
            lines.forEachIndexed { idx, rawLine ->
                if (rawLine.trim().startsWith("@")) {
                    totalSubtasks++
                    if (savedMap["checkbox_$idx"] == true) doneSubtasks++
                }
            }

            // Animated removal similar to HomeScreen rules for today; for YDAY/TMRW/WKEND keep visible even if completed
            val showAnimated = if (key.uppercase() == "TODAY") !isCompleted else true

            if (showAnimated) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    QuestTile(
                        title = baseQuest.title,
                        duration = durationText,
                        subtaskProgress = if (totalSubtasks > 0) "${doneSubtasks}/${totalSubtasks}" else null,
                        hasCalendarMark = baseQuest.calendar_event_id != null,
                        startTime = startText,
                        endTime = endText,
                        deadlineTime = deadlineTextOnly,
                        containerColor = containerColor,
                        statusColor = statusColor,
                        enabled = enabled,
                        onClick = { viewQuest(baseQuest, navController) },
                        onPlay = { viewQuest(baseQuest, navController) },
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .height(tileHeight)
                    )
                }
            }
        }
    }
}
