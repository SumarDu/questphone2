package launcher.launcher.ui.screens.quest.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.data.quest.QuestStatUS
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun GitHubContributionChart(
    contributions: List<QuestStatUS>,
    modifier: Modifier = Modifier
) {
    val weeks = contributions.groupBy { it.date.with(DayOfWeek.MONDAY) }
    val months = contributions.groupBy { it.date.month }
    val horizontalScrollState = rememberScrollState()

    var selectedQuestStatUS by remember { mutableStateOf<QuestStatUS?>(null) }

    Column(modifier = modifier) {
        // Scrollable content
        Column(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .padding(end = 16.dp)
        ) {
            // Month labels
            Row(
                modifier = Modifier
                    .padding(start = 36.dp)
                    .width(maxOf((weeks.size * 15 + 10).dp, 300.dp)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                months.keys.distinct().forEach { month ->
                    Text(
                        text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Day of week labels
                Column(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    DayOfWeek.entries.forEachIndexed { index, day ->
                        if (index % 2 == 0 && day != DayOfWeek.SUNDAY) {
                            Text(
                                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }

                // Contribution grid
                Box(
                    modifier = Modifier
                        .width(maxOf(weeks.size * 15, 300).dp)
                        .height((7 * 17).dp)
                ) {
                    val allDays = mutableListOf<QuestStatUS>()

                    // Fill in all days for proper grid layout
                    val startDate = weeks.keys.minOrNull() ?: LocalDate.now()
                    val endDate = weeks.keys.maxOrNull()?.plusDays(6) ?: LocalDate.now()

                    var currentDate = startDate
                    while (!currentDate.isAfter(endDate)) {
                        val existingDay = contributions.find { it.date == currentDate }
                        if (existingDay != null) {
                            allDays.add(existingDay)
                        } else {
                            allDays.add(QuestStatUS(currentDate, 0,0))
                        }
                        currentDate = currentDate.plusDays(1)
                    }

                    // Group by week for column layout
                    val daysByWeek = allDays.groupBy { it.date.with(DayOfWeek.MONDAY) }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        daysByWeek.keys.sorted().forEach { weekStart ->
                            val weekDays = daysByWeek[weekStart] ?: emptyList()
                            Column(
                                modifier = Modifier.width(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Sort by day of week (Monday to Sunday)
                                val sortedDays = weekDays.sortedBy { it.date.dayOfWeek.value }
                                sortedDays.forEach { day ->
                                    ContributionCell(day,onClick = { clickedDay ->
                                        selectedQuestStatUS = clickedDay
                                    })
                                }
                            }
                        }
                    }

                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Show tooltip when a cell is selected
        selectedQuestStatUS?.let { QuestStatUS ->
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                QuestTooltip(QuestStatUS = QuestStatUS)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            listOf(0, 1, 2, 3, 4).forEach { level ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(getContributionColor(level))
                        .padding(2.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = "More",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ContributionCell(day: QuestStatUS,
                     onClick: ((QuestStatUS) -> Unit)? = null) {
// Calculate completion percentage
    val completionPercentage = if (day.totalQuests > 0) {
        day.completedQuests.toFloat() / day.totalQuests.toFloat()
    } else {
        0f
    }

    // Determine level based on completion percentage
    val level = when {
        day.completedQuests == 0 -> 0 // No activity
        completionPercentage <= 0.25f -> 1 // Low activity
        completionPercentage <= 0.5f -> 2 // Medium activity
        completionPercentage <= 0.75f -> 3 // High activity
        else -> 4 // Very high activity
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(getContributionColor(level))
            .padding(2.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(day) }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center

    ) {

    }
}

@Composable
fun getContributionColor(level: Int): Color {
    return when (level) {
        0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun QuestTooltip(QuestStatUS: QuestStatUS) {
    Box(
        modifier = Modifier
            .background(Color(0xFF161B22), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        val dateText = QuestStatUS.date.format(formatter)
        val percentage = if (QuestStatUS.totalQuests > 0) {
            (QuestStatUS.completedQuests.toFloat() / QuestStatUS.totalQuests.toFloat() * 100).toInt()
        } else 0

        Column {
            Text(
                text = dateText,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${QuestStatUS.completedQuests} of ${QuestStatUS.totalQuests} quests completed ($percentage%)",
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}
