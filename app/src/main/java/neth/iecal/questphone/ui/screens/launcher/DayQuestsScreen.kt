package neth.iecal.questphone.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
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
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.quest.focus.DeepFocus
import neth.iecal.questphone.utils.json
import java.util.concurrent.TimeUnit

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

@OptIn(ExperimentalMaterial3Api::class)
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

    val planned = remember(allQuests, dates, key) {
        allQuests.filter { q ->
            !q.is_destroyed &&
            dates.any { d -> SchedulingUtils.isQuestAvailableOnDate(q.scheduling_info, d) }
        }
    }

    val completedTitles = remember { mutableStateListOf<String>() }
    completedTitles.clear()
    completedTitles.addAll(planned.filter { it.last_completed_on == getCurrentDate() }.map { it.title })

    val tileHeight = 72.dp
    val tileSpacing = 12.dp

    val titleText = when (key.uppercase()) {
        "YDAY" -> "Yesterday"
        "TODAY" -> "Today"
        "TMRW" -> "Tomorrow"
        "WKEND" -> "Weekend"
        else -> key
    }

    val sortedQuests = planned.sortedWith(
        compareBy(
            { if (isAllDayRange(it.time_range)) 1 else 0 },
            { toMinutesRange(it.time_range).first }
        )
    )

    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val todayDate = LocalDate.now().format(dateFormatter)
    val yesterdayDate = LocalDate.now().minusDays(1).format(dateFormatter)

    Scaffold(
        topBar = { TopAppBar(title = { Text(titleText) }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(tileSpacing),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(sortedQuests, key = { it.id }) { baseQuest ->
                val (startMin, endMin) = toMinutesRange(baseQuest.time_range)
                val isCompleted = completedTitles.contains(baseQuest.title)

                val statusColor = when (baseQuest.priority) {
                    QuestPriority.IMPORTANT_URGENT -> Color(0xFFEF4444)
                    QuestPriority.IMPORTANT_NOT_URGENT -> Color(0xFF10B981)
                    QuestPriority.NOT_IMPORTANT_URGENT -> Color(0xFFF5DEB3)
                    QuestPriority.STABLE -> Color(0xFFD1D5DB) // light gray fill for stable
                    QuestPriority.NOT_IMPORTANT_NOT_URGENT -> Color.Transparent
                }
                val statusBorderColor: Color? = when (baseQuest.priority) {
                    QuestPriority.NOT_IMPORTANT_NOT_URGENT -> Color(0xFFD1D5DB)
                    else -> null
                }

                val enabled = true

                val durationText = if (baseQuest.integration_id == IntegrationId.DEEP_FOCUS) {
                    try {
                        val deepFocus = json.decodeFromString<neth.iecal.questphone.data.quest.focus.DeepFocus>(baseQuest.quest_json)
                        val regularSessions = deepFocus.minWorkSessions
                        val extraSessions = deepFocus.maxWorkSessions - deepFocus.minWorkSessions
                        val sessionMinutes = if (deepFocus.nextFocusDurationInMillis > 0) {
                            TimeUnit.MILLISECONDS.toMinutes(deepFocus.nextFocusDurationInMillis).toInt()
                        } else {
                            deepFocus.focusTimeConfig.initialTime.toIntOrNull() ?: 1
                        }
                        val sessionDuration = formatDuration(sessionMinutes)
                        "$regularSessions;$extraSessions * $sessionDuration"
                    } catch (e: Exception) {
                        formatDuration(baseQuest.quest_duration_minutes)
                    }
                } else {
                    formatDuration(baseQuest.quest_duration_minutes)
                }
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
                val checkDateKey = if (key.uppercase() == "YDAY") yesterdayDate else todayDate
                val stateKey = "${baseQuest.id}_${checkDateKey}"
                val savedStatesStr = sp.getString(stateKey, "") ?: ""
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

                // Markers: TODAY => DONE if completed today; YDAY => DONE if completed yesterday else IGNORED
                val marker: String? = when (key.uppercase()) {
                    "TODAY" -> if (baseQuest.last_completed_on == todayDate) "DONE" else null
                    "YDAY" -> if (baseQuest.last_completed_on == yesterdayDate) "DONE" else "IGNORED"
                    else -> null
                }

                val subtaskChip = if (totalSubtasks > 0) "${doneSubtasks}/${totalSubtasks}" else null
                // Marker color mapping for center label
                val markerColor = when (marker) {
                    "DONE" -> Color(0xFF10B981) // green
                    "IGNORED" -> Color(0xFFF59E0B) // amber
                    else -> Color.Transparent
                }

                // Always show items for all filters; we rely on marker to indicate completion/ignored
                val showItem = true

                if (showItem) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        QuestTile(
                            title = baseQuest.title,
                            duration = durationText,
                            subtaskProgress = subtaskChip,
                            coinReward = when {
                                (baseQuest.reward_min > 0 || baseQuest.reward_max > 0) -> if (baseQuest.reward_min == baseQuest.reward_max) "${baseQuest.reward_min}" else "${baseQuest.reward_min}-${baseQuest.reward_max}"
                                else -> null
                            },
                            hasCalendarMark = baseQuest.calendar_event_id != null,
                            startTime = startText,
                            endTime = endText,
                            deadlineTime = deadlineTextOnly,
                            containerColor = containerColor,
                            statusColor = statusColor,
                            statusBorderColor = statusBorderColor,
                            enabled = enabled,
                            onClick = { viewQuest(baseQuest, navController) },
                            onPlay = { viewQuest(baseQuest, navController) },
                            modifier = Modifier
                                .fillMaxWidth(0.94f)
                                .height(tileHeight)
                        )
                        if (marker != null) {
                            Text(
                                text = marker,
                                color = markerColor,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
