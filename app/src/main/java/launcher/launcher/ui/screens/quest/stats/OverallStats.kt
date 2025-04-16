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
import launcher.launcher.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import launcher.launcher.data.DayOfWeek
import launcher.launcher.data.game.User
import launcher.launcher.data.quest.OverallStatsUs
import launcher.launcher.ui.screens.quest.stats.components.GitHubContributionChart
import launcher.launcher.utils.convertToDayOfWeek

@Composable
fun OverallStatsView() {
    val questHelper = QuestHelper(LocalContext.current)
    var contributions by remember { mutableStateOf(emptyList<OverallStatsUs>()) }

    var scrollState = rememberScrollState()
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
                    .padding(horizontal = 16.dp, vertical = 32.dp)
                    .verticalScroll(scrollState),
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

                GeneralStats(contributions)

                QuestPerformanceInsightsSection(contributions)

            }
        }
    )
}

@Composable
private fun KeyMetricsSection(contributions: List<OverallStatsUs>) {
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
                    StatItem(
                        value = "${User.streakData.currentStreak}",
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
private fun GeneralStats(contributions: List<OverallStatsUs>) {
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
                text = "General Stats",
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

private fun calculateTrend(contributions: List<OverallStatsUs>): Float {
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


@Composable
private fun QuestPerformanceInsightsSection(contributions: List<OverallStatsUs>) {
    val questHelper = QuestHelper(LocalContext.current)
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
                text = "Quest Performance Insights (Last 30 days)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )

            // Time Discipline Score
            var showTimeTooltip by remember { mutableStateOf(false) }
            StatRowWithTooltip(
                label = "Time Discipline Score",
                value = "${calculateTimeDisciplineScore(contributions, questHelper)}/100",
                valueColor = when {
                    calculateTimeDisciplineScore(contributions, questHelper) >= 80 -> MaterialTheme.colorScheme.primary
                    calculateTimeDisciplineScore(contributions, questHelper) >= 50 -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.error
                },
                showTooltip = showTimeTooltip,
                onTooltipToggle = { showTimeTooltip = !showTimeTooltip },
                tooltipText = "How often you complete quests within their set time range. Higher means better schedule adherence."
            )

            // Day-of-Week Powerhouse
            var showPowerhouseTooltip by remember { mutableStateOf(false) }
            StatRowWithTooltip(
                label = "Powerhouse Day",
                value = calculatePowerhouseDay(contributions) ?: "Not enough data",
                valueColor = MaterialTheme.colorScheme.onSurface,
                showTooltip = showPowerhouseTooltip,
                onTooltipToggle = { showPowerhouseTooltip = !showPowerhouseTooltip },
                tooltipText = "The day of the week you complete the most quests on average."
            )

            // Failure Recovery Rate
            var showFailureTooltip by remember { mutableStateOf(false) }
            StatRowWithTooltip(
                label = "Failure Recovery Rate",
                value = "${calculateFailureRecoveryRate(contributions, questHelper)}%",
                valueColor = when {
                    calculateFailureRecoveryRate(contributions, questHelper) >= 70 -> MaterialTheme.colorScheme.primary
                    calculateFailureRecoveryRate(contributions, questHelper) >= 40 -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.error
                },
                showTooltip = showFailureTooltip,
                onTooltipToggle = { showFailureTooltip = !showFailureTooltip },
                tooltipText = "How often you complete quests after failing them earlier. Higher means better resilience."
            )

            // Missed Reward Opportunity
            var showMissedTooltip by remember { mutableStateOf(false) }
            StatRowWithTooltip(
                label = "Missed Rewards",
                value = "${calculateMissedRewardOpportunity(contributions, questHelper)} points",
                valueColor = if (calculateMissedRewardOpportunity(contributions, questHelper) > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                showTooltip = showMissedTooltip,
                onTooltipToggle = { showMissedTooltip = !showMissedTooltip },
                tooltipText = "Total rewards lost from uncompleted quests. Complete more to earn these points!"
            )

            Text(
                text = "Stick to schedules, repeat quests, and recover from misses to maximize rewards!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatRowWithTooltip(
    label: String,
    value: String,
    valueColor: Color,
    showTooltip: Boolean,
    onTooltipToggle: () -> Unit,
    tooltipText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Box {
                Icon(
                    painter = painterResource(id = R.drawable.outline_help_24),
                    contentDescription = "Help",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onTooltipToggle() },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DropdownMenu(
                    expanded = showTooltip,
                    onDismissRequest = { onTooltipToggle() },
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = tooltipText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}
private fun calculateTimeDisciplineScore(contributions: List<OverallStatsUs>, questHelper: QuestHelper): Int {
    val completedQuests = contributions
        .filter { it.date >= LocalDate.now().minusDays(30) }
        .flatMap { day -> questHelper.getCompletedQuestsForDay(day.date) }
    if (completedQuests.isEmpty()) return 0
    // Assume completion is within timeRange (no completion time data; uses isInTimeRange logic)
    val withinTimeRange = completedQuests.count { quest ->
        quest.timeRange.let { range ->
            range.getOrElse(0) { 0 } <= range.getOrElse(1) { 24 }
        }
    }
    return (withinTimeRange.toFloat() / completedQuests.size * 100).toInt()
}

private fun calculatePowerhouseDay(contributions: List<OverallStatsUs>): String? {
    val recentContributions = contributions
        .filter { it.date >= LocalDate.now().minusDays(30) }
        .groupBy { contribution -> contribution.date.dayOfWeek.convertToDayOfWeek()
        }
        .mapValues { entry ->
            entry.value.map { it.completedQuests }.average()
        }
    if (recentContributions.isEmpty()) return null
    return recentContributions.maxByOrNull { it.value }?.key?.name
}

private fun calculateFailureRecoveryRate(contributions: List<OverallStatsUs>, questHelper: QuestHelper): Int {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val failedQuests = contributions
        .filter { it.date >= LocalDate.now().minusDays(30) }
        .flatMap { day -> questHelper.getFailedQuestsForDay(day.date).map { it to day.date } }
    if (failedQuests.isEmpty()) return 100
    val recoveredQuests = failedQuests.count { (quest, failedDate) ->
        val destructDate = LocalDate.parse(quest.autoDestruct, formatter)
        contributions.any { contribution ->
            contribution.date > failedDate &&
                    contribution.date <= destructDate &&
                    quest.selectedDays.contains(
                        contribution.date.dayOfWeek.convertToDayOfWeek()
                    ) &&
                    questHelper.getCompletedQuestsForDay(contribution.date).any { it.title == quest.title }
        }
    }
    return (recoveredQuests.toFloat() / failedQuests.size * 100).toInt()
}

private fun calculateMissedRewardOpportunity(contributions: List<OverallStatsUs>, questHelper: QuestHelper): Int {
    return contributions
        .filter { it.date >= LocalDate.now().minusDays(30) }
        .flatMap { day -> questHelper.getFailedQuestsForDay(day.date) }
        .sumOf { it.reward }
}