package launcher.launcher.ui.screens.quest.stats

import androidx.compose.runtime.Composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import launcher.launcher.utils.QuestHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.LaunchedEffect
import launcher.launcher.data.quest.QuestStatUS
import launcher.launcher.ui.screens.quest.stats.components.GitHubContributionChart
import kotlin.math.pow

@Composable
fun QuestStatsView() {
    val questHelper = QuestHelper(LocalContext.current)
    var contributions by remember { mutableStateOf(emptyList<QuestStatUS>()) }

    LaunchedEffect (contributions) {
        contributions = questHelper.getOverallStats()
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Contribution Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    GitHubContributionChart(
                        contributions = contributions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                // Key Metrics
                KeyMetricsSection(contributions)

                // Performance Insights
                PerformanceInsightsSection(contributions)

            }
        }
    )
}

@Composable
private fun KeyMetricsSection(contributions: List<QuestStatUS>) {
    AnimatedVisibility(
        visible = contributions.isNotEmpty(),
        enter = fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Key Metrics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Average Completion Rate
                    val completionRate = contributions
                        .filter { it.totalQuests > 0 }
                        .map { it.completedQuests.toFloat() / it.totalQuests }
                        .average()
                        .times(100)
                        .toInt()
                    StatItem(
                        value = "$completionRate%",
                        label = "Completion Rate",
                        modifier = Modifier.weight(1f)
                    )

                    // Current Streak
                    val currentStreak = calculateStreak(contributions)
                    StatItem(
                        value = "$currentStreak",
                        label = "Current Streak",
                        modifier = Modifier.weight(1f)
                    )

                    // Best Day
                    val bestDay = contributions.maxByOrNull { it.completedQuests }
                    val formattedBestDate = bestDay?.date?.format(DateTimeFormatter.ofPattern("MMM d")) ?: "N/A"
                    StatItem(
                        value = "${bestDay?.completedQuests ?: 0}",
                        label = "Best Day\n($formattedBestDate)",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceInsightsSection(contributions: List<QuestStatUS>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Performance Insights",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )

            // Weekly Average
            val weeklyAvg = contributions
                .filter { it.date >= LocalDate.now().minusWeeks(1) }
                .map { it.completedQuests }
                .average()
                .toInt()
            StatRow(label = "Weekly Avg", value = "$weeklyAvg quests")

            // Monthly Average
            val monthlyAvg = contributions
                .filter { it.date >= LocalDate.now().minusMonths(1) }
                .map { it.completedQuests }
                .average()
                .toInt()
            StatRow(label = "Monthly Avg", value = "$monthlyAvg quests")

            // Productivity Trend
            val trend = calculateTrend(contributions)
            StatRow(
                label = "Trend (Last 30 Days)",
                value = when {
                    trend > 0 -> "Improving (+${trend.toInt()}%)"
                    trend < 0 -> "Declining (${trend.toInt()}%)"
                    else -> "Stable"
                },
                valueColor = when {
                    trend > 0 -> MaterialTheme.colorScheme.primary
                    trend < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun ConsistencyScoreSection(contributions: List<QuestStatUS>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Consistency Score",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )
            val consistencyScore = calculateConsistencyScore(contributions)
            StatRow(
                label = "Daily Consistency (Last 30 Days)",
                value = "${consistencyScore}/100",
                valueColor = when {
                    consistencyScore >= 80 -> MaterialTheme.colorScheme.primary
                    consistencyScore >= 50 -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.error
                }
            )
            Text(
                text = "Lower variation in daily completions means a higher score. Aim for steady progress!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun calculateConsistencyScore(contributions: List<QuestStatUS>): Int {
    val recentContributions = contributions
        .filter { it.date >= LocalDate.now().minusDays(30) }
        .map { it.completedQuests.toDouble() }
    if (recentContributions.isEmpty()) return 0
    val mean = recentContributions.average()
    val standardDeviation = if (recentContributions.size > 1) {
        Math.sqrt(recentContributions.map { (it - mean).pow(2) }.average())
    } else 0.0
    // Normalize to 0-100 scale; lower deviation = higher score
    val maxDeviation = mean * 2 // Arbitrary cap for scaling
    return if (maxDeviation == 0.0) 100 else {
        ((1 - (standardDeviation / maxDeviation)) * 100).coerceIn(0.0, 100.0).toInt()
    }
}
@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

private fun calculateStreak(contributions: List<QuestStatUS>): Int {
    if (contributions.isEmpty()) return 0
    var streak = 0
    var currentDate = LocalDate.now()
    val sortedContributions = contributions.sortedByDescending { it.date }

    for (day in sortedContributions) {
        if (day.date == currentDate && day.completedQuests > 0) {
            streak++
            currentDate = currentDate.minusDays(1)
        } else if (day.date < currentDate) {
            break
        }
    }
    return streak
}

private fun calculateTrend(contributions: List<QuestStatUS>): Float {
    if (contributions.size < 14) return 0f
    val recent = contributions
        .filter { it.date >= LocalDate.now().minusDays(14) }
        .map { it.completedQuests }
        .average()
    val previous = contributions
        .filter { it.date in LocalDate.now().minusDays(28)..LocalDate.now().minusDays(15) }
        .map { it.completedQuests }
        .average()
    return if (previous.isNaN() || recent.isNaN()) 0f else ((recent - previous) / previous * 100).toFloat()
}
