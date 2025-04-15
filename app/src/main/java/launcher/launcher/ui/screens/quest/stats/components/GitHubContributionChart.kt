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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
        Column(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .padding(end = 16.dp)
        ) {
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

                Box(
                    modifier = Modifier
                        .width(maxOf(weeks.size * 15, 300).dp)
                        .height((7 * 17).dp)
                ) {
                    val allDays = mutableListOf<QuestStatUS>()

                    val startDate = weeks.keys.minOrNull() ?: LocalDate.now()
                    val endDate = weeks.keys.maxOrNull()?.plusDays(6) ?: LocalDate.now()

                    var currentDate = startDate
                    while (!currentDate.isAfter(endDate)) {
                        val existingDay = contributions.find { it.date == currentDate }
                        if (existingDay != null) {
                            allDays.add(existingDay)
                        } else {
                            allDays.add(QuestStatUS(currentDate, 0, 0))
                        }
                        currentDate = currentDate.plusDays(1)
                    }

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
                                val sortedDays = weekDays.sortedBy { it.date.dayOfWeek.value }
                                sortedDays.forEach { day ->
                                    ContributionCell(day, onClick = { clickedDay ->
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

        selectedQuestStatUS?.let { questStatUS ->
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                QuestTooltip(questStatUS = questStatUS)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
fun ContributionCell(
    day: QuestStatUS,
    onClick: ((QuestStatUS) -> Unit)? = null
) {
    val completionPercentage = if (day.totalQuests > 0) {
        day.completedQuests.toFloat() / day.totalQuests.toFloat()
    } else {
        0f
    }

    val level = when {
        day.completedQuests == 0 -> 0
        completionPercentage <= 0.25f -> 1
        completionPercentage <= 0.5f -> 2
        completionPercentage <= 0.75f -> 3
        else -> 4
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
    ){}
}

@Composable
fun getContributionColor(level: Int): Color {
    return when (level) {
        0 -> MaterialTheme.colorScheme.surfaceVariant // Grey for no activity
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun QuestTooltip(questStatUS: QuestStatUS) {
    Card(
        modifier = Modifier
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
            val dateText = questStatUS.date.format(formatter)
            val percentage = if (questStatUS.totalQuests > 0) {
                (questStatUS.completedQuests.toFloat() / questStatUS.totalQuests.toFloat() * 100).toInt()
            } else 0

            Text(
                text = dateText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${questStatUS.completedQuests}/${questStatUS.totalQuests} Quests",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "($percentage%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (percentage > 50) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            // Optional: Add a status indicator
            Text(
                text = when {
                    percentage == 0 -> "No activity"
                    percentage == 100 -> "All quests completed!"
                    percentage > 50 -> "Great progress"
                    else -> "Keep going!"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}