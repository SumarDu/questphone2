package launcher.launcher.ui.screens.quest.stats

import androidx.compose.runtime.Composable

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.random.Random

@Composable
fun QuestStatsView(){
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
                .padding(16.dp)
        ) {
            val today = LocalDate.now()
            val startDate = today.minusDays(365)

            val contributions = mutableListOf<QuestDay>()
            var currentDate = startDate

            while (!currentDate.isAfter(today)) {
                // Generate random contributions with some patterns
                val count = when {
                    currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY ->
                        Random.nextInt(0, 3)
                    currentDate.dayOfMonth % 7 == 0 ->
                        Random.nextInt(5, 12)
                    currentDate.dayOfMonth % 5 == 0 ->
                        Random.nextInt(3, 8)
                    else ->
                        Random.nextInt(0, 5)
                }

                contributions.add(QuestDay(currentDate, count))
                currentDate = currentDate.plusDays(1)
            }

            Text("Statistics",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.size(32.dp))

            GitHubContributionChart(
                contributions = contributions,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.size(16.dp))

            Text("Best Day: 4th May",
                style = MaterialTheme.typography.bodyLarge)

            Text("Worst Day: 5th May",
                style = MaterialTheme.typography.bodyLarge)

            Text("Discipline Rating: 5/10",
                style = MaterialTheme.typography.bodyLarge)

//            Text("Discipline: 5/10",
//                style = MaterialTheme.typography.bodyLarge)

        }
    }
}

data class QuestDay(
    val date: LocalDate,
    val questCount: Int
)

@Composable
fun GitHubContributionChart(
    contributions: List<QuestDay>,
    modifier: Modifier = Modifier
) {
    val weeks = contributions.groupBy { it.date.with(DayOfWeek.MONDAY) }
    val months = contributions.groupBy { it.date.month }
    val horizontalScrollState = rememberScrollState()

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
                    val allDays = mutableListOf<QuestDay>()

                    // Fill in all days for proper grid layout
                    val startDate = weeks.keys.minOrNull() ?: LocalDate.now()
                    val endDate = weeks.keys.maxOrNull()?.plusDays(6) ?: LocalDate.now()

                    var currentDate = startDate
                    while (!currentDate.isAfter(endDate)) {
                        val existingDay = contributions.find { it.date == currentDate }
                        if (existingDay != null) {
                            allDays.add(existingDay)
                        } else {
                            allDays.add(QuestDay(currentDate, 0))
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
                                    ContributionCell(day)
                                }
                            }
                        }
                    }
                }
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
fun ContributionCell(day: QuestDay) {
    val level = when (day.questCount) {
        0 -> 0
        in 1..3 -> 1
        in 4..6 -> 2
        in 7..9 -> 3
        else -> 4
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(getContributionColor(level))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Tooltip functionality would be added here in a real implementation
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun GitHubContributionChartPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GitHub Contributions") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { paddingValues ->
            ContributionScreen(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
fun ContributionScreen(modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val startDate = today.minusDays(365)

    val contributions = mutableListOf<QuestDay>()
    var currentDate = startDate

    while (!currentDate.isAfter(today)) {
        // Generate random contributions with some patterns
        val count = when {
            currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY ->
                Random.nextInt(0, 3)
            currentDate.dayOfMonth % 7 == 0 ->
                Random.nextInt(5, 12)
            currentDate.dayOfMonth % 5 == 0 ->
                Random.nextInt(3, 8)
            else ->
                Random.nextInt(0, 5)
        }

        contributions.add(QuestDay(currentDate, count))
        currentDate = currentDate.plusDays(1)
    }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "Sarah's Contribution Activity",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val totalContributions = contributions.sumOf { it.questCount }

                Text(
                    text = "$totalContributions contributions in the last year",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                GitHubContributionChart(
                    contributions = contributions,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Activity summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Contribution Summary",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val avgWeekday = contributions
                            .filter { it.date.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
                            .map { it.questCount }
                            .average()
                            .toInt()

                        Text(
                            text = "$avgWeekday",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Avg on weekdays",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val avgWeekend = contributions
                            .filter { it.date.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
                            .map { it.questCount }
                            .average()
                            .toInt()

                        Text(
                            text = "$avgWeekend",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Avg on weekends",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val bestDay = contributions.maxByOrNull { it.questCount }
                        val formattedDate = bestDay?.date?.format(DateTimeFormatter.ofPattern("MMM d")) ?: ""

                        Text(
                            text = "${bestDay?.questCount ?: 0}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Best day ($formattedDate)",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun max(a: androidx.compose.ui.unit.Dp, b: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
    return if (a > b) a else b
}