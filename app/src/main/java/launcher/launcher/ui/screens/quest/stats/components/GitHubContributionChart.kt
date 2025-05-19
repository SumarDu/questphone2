package launcher.launcher.ui.screens.quest.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import launcher.launcher.data.quest.OverallStatsUs // Assuming this class exists
import launcher.launcher.data.quest.QuestDatabaseProvider
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.filterQuestsByDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// --- Constants for easy adjustments ---
private val CELL_SIZE = 12.dp
private val CELL_SPACING = 3.dp
private val MONTH_SPACING = 8.dp // Space between months in the grid
private val DAY_LABEL_WIDTH = 32.dp
private val MONTH_LABEL_HEIGHT = 20.dp
private val DAY_LABEL_VERTICAL_SPACING = CELL_SIZE + CELL_SPACING

@Composable
fun GitHubContributionChart(
    contributions: List<OverallStatsUs>,
    modifier: Modifier = Modifier
) {
    if (contributions.isEmpty()) {
        Text("No contribution data available.", modifier = modifier.padding(16.dp))
        return
    }

    val contributionsMap = remember(contributions) {
        contributions.associateBy { it.date }
    }

    // Determine the date range and pad with empty days
    val dateRange = remember(contributions) {
        val minDate = contributions.minOf { it.date }
        val maxDate = contributions.maxOf { it.date }
        // Start from the beginning of the week containing the first contribution
        val startDate = minDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        // End at the end of the week containing the last contribution
        val endDate = maxDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        startDate to endDate
    }

    val allDaysData = remember(dateRange, contributionsMap) {
        val (startDate, endDate) = dateRange
        val days = mutableListOf<OverallStatsUs>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            days.add(
                contributionsMap[currentDate] ?: OverallStatsUs(currentDate, 0, 0) // Use existing or default
            )
            currentDate = currentDate.plusDays(1)
        }
        days
    }

    // Group padded days by week (starting Monday)
    val weeksData = remember(allDaysData) {
        allDaysData.groupBy { it.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .toSortedMap() // Ensure weeks are ordered
    }

    // Group weeks by the month they primarily belong to (or start in) for label positioning
    val monthLabelsData = remember(weeksData) {
        weeksData.keys.groupBy { it.month }
            .mapValues { entry -> entry.value.size } // Month -> Number of weeks starting in it
            .toList()
            .sortedBy { it.first } // Sort months chronologically
    }

    val horizontalScrollState = rememberScrollState()
    var selectedOverallStatsUs by remember { mutableStateOf<OverallStatsUs?>(null) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            DayLabelsColumn()

            Column {
                MonthLabelsRow(monthLabelsData = monthLabelsData)

                Spacer(modifier = Modifier.height(4.dp))

                ContributionGrid(
                    weeksData = weeksData,
                    onCellClick = { selectedOverallStatsUs = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth() // Allow tooltip to center itself
                .padding(horizontal = 16.dp), // Add padding if needed
            contentAlignment = Alignment.Center
        ) {
            selectedOverallStatsUs?.let { questStatUS ->
                QuestTooltip(overallStatsUs = questStatUS)
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        ContributionLegend(modifier = Modifier.padding(horizontal = 16.dp))
    }
}


@Composable
private fun DayLabelsColumn(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(DAY_LABEL_WIDTH)
            .padding(top = MONTH_LABEL_HEIGHT + 4.dp), // Align below month labels
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CELL_SPACING) // Match grid spacing
    ) {
        // Display labels for Monday, Wednesday, Friday
        val daysToShow = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY,DayOfWeek.SUNDAY)
        DayOfWeek.entries.forEach { day ->
            if (day in daysToShow) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.height(CELL_SIZE) // Match cell height
                )
            } else {
                // Spacer to maintain alignment with grid rows
                Spacer(modifier = Modifier.height(CELL_SIZE))
            }
        }
    }
}

@Composable
private fun MonthLabelsRow(
    monthLabelsData: List<Pair<Month, Int>> // List of (Month, numberOfWeeks)
) {
    Row(
        modifier = Modifier.height(MONTH_LABEL_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var firstMonth = true
        monthLabelsData.forEach { (month, weekCount) ->

            // Width of the actual cells in this month's section
            val columnsWidth = CELL_SIZE * weekCount
            // Width of the gaps between weeks WITHIN this month's section
            val internalSpacingWidth = CELL_SPACING * (weekCount - 1).coerceAtLeast(0)
            // Total width of the grid elements corresponding to this month
            val monthSectionGridWidth = columnsWidth + internalSpacingWidth


            // Add spacing *before* the month label
            if (!firstMonth) {
                // Use the designated space between month sections
                Spacer(modifier = Modifier.width(MONTH_SPACING))
            } else {
                // Small initial spacer for the very first label for rough alignment
                Spacer(modifier = Modifier.width(CELL_SIZE / 2)) // Heuristic alignment offset
                firstMonth = false
            }

            Text(
                text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.wrapContentWidth() // Let text take its natural width
            )

            // Add spacer *after* the text to push the next label roughly to the start of the next month section.
            // This is approximate positioning.
            // We want the space after this label + the space before the next label (MONTH_SPACING)
            // to roughly equal the grid width calculated above.
            // spaceAfterLabel + (MONTH_SPACING or CELL_SIZE/2 for first) ~= monthSectionGridWidth
            val spaceNeededBeforeNextLabel = if(monthLabelsData.last().first == month) 0.dp else MONTH_SPACING // No space needed after the last label
            val spaceAfterLabel = (monthSectionGridWidth - (CELL_SIZE / 2) - spaceNeededBeforeNextLabel).coerceAtLeast(0.dp) // Subtract initial offset and the next gap

            Spacer(modifier = Modifier.width(spaceAfterLabel))
        }
    }
}

@Composable
private fun ContributionGrid(
    weeksData: Map<LocalDate, List<OverallStatsUs>>,
    onCellClick: (OverallStatsUs) -> Unit
) {
    Row(
        // horizontalArrangement = Arrangement.spacedBy(CELL_SPACING) // Base spacing between columns
    ) {
        val sortedWeeks = weeksData.keys.toList() // Already sorted map keys
        sortedWeeks.forEachIndexed { index, weekStartDate ->
            val weekDays = weeksData[weekStartDate] ?: emptyList()
            val daysInWeekMap = weekDays.associateBy { it.date.dayOfWeek }

            Column(
                verticalArrangement = Arrangement.spacedBy(CELL_SPACING)
            ) {
                // Ensure 7 cells per week, even if data is missing for some days
                DayOfWeek.entries.forEach { dayOfWeek ->
                    val dateForCell = weekStartDate.plusDays(dayOfWeek.value.toLong() - DayOfWeek.MONDAY.value)
                    val dayData = daysInWeekMap[dayOfWeek] ?: OverallStatsUs(dateForCell, 0, 0, isPlaceholder = true) // Mark placeholders if needed
                    ContributionCell(day = dayData, onClick = onCellClick)
                }
            }

            val nextWeekStartDate = sortedWeeks.getOrNull(index + 1)
            if (nextWeekStartDate != null && nextWeekStartDate.month != weekStartDate.month) {
                Spacer(modifier = Modifier.width(MONTH_SPACING))
            } else if (index < sortedWeeks.size - 1) {
                // Regular spacing between weeks within the same month
                Spacer(modifier = Modifier.width(CELL_SPACING))
            }
        }
    }
}


@Composable
fun ContributionCell(
    day: OverallStatsUs,
    onClick: ((OverallStatsUs) -> Unit)
) {
    // Check if it's a placeholder added just for grid structure
    // val isPlaceholder = day.totalQuests == 0 && day.completedQuests == 0 // Or add a flag to OverallStatsUs
    val isPlaceholder = day.isPlaceholder // Assuming you add this flag

    val contributionLevel = if (isPlaceholder || day.totalQuests <= 0) {
        0 // No activity or placeholder
    } else {
        when ((day.completedQuests.toFloat() / day.totalQuests.toFloat())) {
            0f -> 0 // Explicitly 0 if completed is 0 but total wasn't
            in 0f..0.25f -> 1
            in 0.25f..0.5f -> 2
            in 0.5f..0.75f -> 3
            else -> 4 // More than 0.75 up to 1.0
        }
    }

    Box(
        modifier = Modifier
            .size(CELL_SIZE)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(getContributionColor(contributionLevel))
            .clickable(enabled = !isPlaceholder) { // Only allow clicking actual data
                onClick(day)
            }
        // .padding(1.dp) // Optional: if you want a tiny border effect inside the cell
    )
}

@Composable
fun getContributionColor(level: Int): Color {
    return when (level) {
        0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Lighter grey for empty/placeholder
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        4 -> MaterialTheme.colorScheme.primary // Max contribution
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Default fallback
    }
}

@Composable
fun ContributionLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        // Levels 0 through 4
        (0..4).forEach { level ->
            Box(
                modifier = Modifier
                    .size(CELL_SIZE) // Match cell size
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(getContributionColor(level))
            )
            Spacer(modifier = Modifier.width(CELL_SPACING)) // Match cell spacing
        }
        // Remove last spacer if needed, or keep for padding
        // Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "More",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}


@Composable
fun QuestTooltip(overallStatsUs: OverallStatsUs) {
    val questHelper = QuestHelper(LocalContext.current)
    val dao = QuestDatabaseProvider.getInstance(LocalContext.current).questDao()

    var questsForDay by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val quest = dao.getAllQuests().first()
        quest.filterQuestsByDay(overallStatsUs.date)
        questsForDay = quest.size
    }

    Card(
        modifier = Modifier
            .padding(8.dp), // Padding around the card
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp), // Padding inside the card
            verticalArrangement = Arrangement.spacedBy(6.dp) // Reduced spacing a bit
        ) {
            val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()) }
            val dateText = overallStatsUs.date.format(formatter)
            val percentage = if (overallStatsUs.totalQuests > 0) {
                (overallStatsUs.completedQuests.toFloat() / overallStatsUs.totalQuests.toFloat() * 100).toInt()
            } else 0

            Text(
                text = dateText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val questText = when {
                overallStatsUs.isPlaceholder -> "No data for this day" // Handle placeholder display
                overallStatsUs.totalQuests == 0 -> "0/${questsForDay} Quests"
                else -> "${overallStatsUs.completedQuests} / ${overallStatsUs.totalQuests} Quests"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = questText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Only show percentage if there were quests
                if (!overallStatsUs.isPlaceholder && overallStatsUs.totalQuests > 0) {
                    Text(
                        text = "($percentage%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            percentage >= 100 -> MaterialTheme.colorScheme.primary // 100% is good
                            percentage > 50 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            percentage > 0 -> MaterialTheme.colorScheme.tertiary // Some progress
                            else -> MaterialTheme.colorScheme.error // 0%
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Optional: Add a status indicator text
            val statusText = when {
                overallStatsUs.isPlaceholder -> "" // No status for placeholders
                percentage == 0 && overallStatsUs.totalQuests > 0 -> "No activity"
                percentage == 0 && overallStatsUs.totalQuests == 0 -> "" // Or "No quests"
                percentage == 100 -> "All quests completed!"
                percentage > 50 -> "Great progress"
                percentage > 0 -> "Keep going!"
                else -> "" // Should not happen if logic above is correct
            }
            if(statusText.isNotEmpty()){
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

