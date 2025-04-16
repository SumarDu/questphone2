package launcher.launcher.ui.screens.quest.stats.specific

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.data.quest.QuestStats
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.formatHour
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.*
import launcher.launcher.R

@Composable
fun BaseQuestStatsView(baseData: BasicQuestInfo) {
    // Calculate statistics
    val questHelper = QuestHelper(LocalContext.current)
    val questStats = questHelper.getQuestStats(baseData)
    val completedQuests = questStats.size
    val successfulQuests = questStats.filter { item -> item.value.isSuccessful }.size
    val failedQuests = questStats.filter { item -> !item.value.isSuccessful }.size
    val currentStreak = calculateCurrentQuestStreak(questStats)
    val longestStreak = calculateLongestQuestStreak(questStats)
    val failureRate = if (completedQuests > 0) (failedQuests.toFloat() / completedQuests) * 100 else 0f
    val successRate = if (completedQuests > 0) (successfulQuests.toFloat() / completedQuests) * 100 else 0f
    val totalCoins = successfulQuests * baseData.reward

    // Find first and last completion dates
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val sortedDates = if (questStats.isNotEmpty()) {
        questStats.keys.map { LocalDate.parse(it, dateFormatter) }.sorted()
    } else {
        listOf()
    }

    val firstCompletionDate = sortedDates.firstOrNull()
    val lastCompletionDate = sortedDates.lastOrNull()
    val daysSinceStart = if (firstCompletionDate != null) {
        ChronoUnit.DAYS.between(firstCompletionDate, LocalDate.now()) + 1
    } else 0

    // Calculate weekly stats
    val weeklyAverageCompletions = calculateWeeklyAverageSuccess(questStats)

    // Handle calendar state
    var showCalendarDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedDateStats by remember { mutableStateOf<QuestStats?>(null) }

    val currentYearMonth = remember { mutableStateOf(YearMonth.now()) }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with quest name and quick stats
            QuestHeader(baseData, currentStreak)

            // Progress overview cards
            ProgressStatsSection(
                successRate = successRate,
                questStats = questStats,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                weeklyAverage = weeklyAverageCompletions,
                daysSinceStart = daysSinceStart,
                totalCoins = totalCoins
            )

            // Calendar preview showing last month's completions
            CalendarSection(
                questStats = questStats,
                onShowFullCalendar = { showCalendarDialog = true }
            )

            // Quest Details
            QuestDetailsCard(baseData)
        }

        // Calendar Dialog
        if (showCalendarDialog) {
            CalendarDialog(
                questStats = questStats,
                currentYearMonth = currentYearMonth,
                onDismiss = { showCalendarDialog = false },
                onDateSelected = { date, stats ->
                    selectedDate = date
                    selectedDateStats = stats
                }
            )
        }

        // Details dialog for selected date
        if (selectedDate != null && selectedDateStats != null) {
            CompletionDetailsDialog(
                date = selectedDate!!,
                stats = selectedDateStats!!,
                onDismiss = {
                    selectedDate = null
                    selectedDateStats = null
                }
            )
        }
    }
}

@Composable
fun QuestHeader(baseData: BasicQuestInfo, currentStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = baseData.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side - Time window
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatHour(baseData.timeRange[0])} - ${formatHour(baseData.timeRange[1])}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                // Right side - Current streak with fire icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_local_fire_department_24
                        ),
                        contentDescription = "Current Streak",
                        tint = if (currentStreak > 0) Color(0xFFF57C00) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentStreak > 0) "$currentStreak days" else "No active streak",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (currentStreak > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (currentStreak > 0)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressStatsSection(
    successRate: Float,
    questStats: Map<String, QuestStats>,
    currentStreak: Int,
    longestStreak: Int,
    weeklyAverage: Double,
    daysSinceStart: Long,
    totalCoins: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Progress Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Circular progress indicator for success rate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedSuccessRate by animateFloatAsState(
                    targetValue = successRate / 100f,
                    label = ""
                )

                CircularProgressIndicator(
                    progress = { animatedSuccessRate },
                    modifier = Modifier.size(150.dp),
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = when {
                        successRate >= 80f -> Color(0xFF4CAF50) // Green
                        successRate >= 60f -> Color(0xFF8BC34A) // Light Green
                        successRate >= 40f -> Color(0xFFFFC107) // Amber
                        successRate >= 20f -> Color(0xFFFF9800) // Orange
                        else -> Color(0xFFF44336) // Red
                    }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${successRate.toInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Success Rate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Key stats in a grid
            val items = listOf(
                Triple(
                    "Longest Streak",
                    "$longestStreak days",
                    R.drawable.baseline_local_fire_department_24
                ),
                Triple(
                    "Weekly Average",
                    "${String.format("%.1f", weeklyAverage)}",
                    R.drawable.baseline_view_week_24
                ),
                Triple("Total Earned", "$totalCoins coins", R.drawable.baseline_circle_24),
                Triple(
                    "Days Active",
                    "${questStats.size}/${daysSinceStart}",
                    R.drawable.baseline_calendar_month_24
                )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items.chunked(2).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { (title, value, icon) ->
                            StatCard(title, value, icon, Modifier.weight(1f))
                        }

                        // Fill empty space if odd number of items
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 4.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun CalendarSection(
    questStats: Map<String, QuestStats>,
    onShowFullCalendar: () -> Unit
) {
    val today = LocalDate.now()
    val pastWeekDates = (0..6).map { today.minusDays(it.toLong()) }.reversed()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onShowFullCalendar) {
                    Text("View Full Calendar")
                }
            }

            // Last 7 days calendar preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                pastWeekDates.forEach { date ->
                    val dateString = date.format(dateFormatter)
                    val stats = questStats[dateString]
                    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val isToday = date.isEqual(today)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Day of week
                        Text(
                            text = dayOfWeek.substring(0, 1),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Date circle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        stats?.isSuccessful == true -> MaterialTheme.colorScheme.primary
                                        stats != null -> MaterialTheme.colorScheme.error
                                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday) 2.dp else 1.dp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    stats?.isSuccessful == true -> MaterialTheme.colorScheme.onPrimary
                                    stats != null -> MaterialTheme.colorScheme.onError
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestDetailsCard(baseData: BasicQuestInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quest Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            QuestInfoRow(label = "Days Active", value = baseData.selectedDays.toString())
            QuestInfoRow(
                label = "Time Range",
                value = "${formatHour(baseData.timeRange[0])} - ${formatHour(baseData.timeRange[1])}"
            )
            QuestInfoRow(label = "Created", value = baseData.createdOn)
            QuestInfoRow(label = "Expires", value = baseData.autoDestruct)
            QuestInfoRow(label = "Integration", value = baseData.integrationId.name)
            QuestInfoRow(
                label = "Reward",
                value = "${baseData.reward} coins",
                highlight = true
            )
        }
    }
}

@Composable
private fun QuestInfoRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun CalendarDialog(
    questStats: Map<String, QuestStats>,
    currentYearMonth: MutableState<YearMonth>,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate, QuestStats) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Calendar header with month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        currentYearMonth.value = currentYearMonth.value.minusMonths(1)
                    }) {
                        Text("<", style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        text = currentYearMonth.value.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                                " " + currentYearMonth.value.year,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(onClick = {
                        currentYearMonth.value = currentYearMonth.value.plusMonths(1)
                    }) {
                        Text(">", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Weekday headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (day in DayOfWeek.values()) {
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).substring(0, 1),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Calendar days
                val firstOfMonth = currentYearMonth.value.atDay(1)
                val daysInMonth = currentYearMonth.value.lengthOfMonth()
                val firstDayOfWeekValue = firstOfMonth.dayOfWeek.value % 7 // Adjusting to make Sunday=0, Monday=1, etc.

                val today = LocalDate.now()

                // Calculate rows needed (include blank spaces and full weeks)
                val totalDays = firstDayOfWeekValue + daysInMonth
                val rows = (totalDays + 6) / 7 // Round up to full weeks

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val dayIndex = row * 7 + col - firstDayOfWeekValue + 1

                                if (dayIndex in 1..daysInMonth) {
                                    val date = currentYearMonth.value.atDay(dayIndex)
                                    val dateString = date.format(dateFormatter)
                                    val stats = questStats[dateString]
                                    val isToday = date.isEqual(today)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    stats?.isSuccessful == true -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                    stats != null -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isToday) 2.dp else 0.dp,
                                                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable(
                                                enabled = stats != null,
                                                onClick = {
                                                    stats?.let { onDateSelected(date, it) }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayIndex.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = when {
                                                stats?.isSuccessful == true -> MaterialTheme.colorScheme.onPrimary
                                                stats != null -> MaterialTheme.colorScheme.onError
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                } else {
                                    // Empty space for days not in this month
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, text = "Completed")
                    LegendItem(color = MaterialTheme.colorScheme.error, text = "Failed")
                    if (today.month == currentYearMonth.value.month && today.year == currentYearMonth.value.year) {
                        LegendItem(borderColor = MaterialTheme.colorScheme.primary, text = "Today")
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color? = null, borderColor: Color? = null, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .then(
                    if (color != null)
                        Modifier.background(color)
                    else if (borderColor != null)
                        Modifier.border(1.dp, borderColor, CircleShape)
                    else
                        Modifier
                )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompletionDetailsDialog(
    date: LocalDate,
    stats: QuestStats,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with date
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Divider()

                // Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (stats.isSuccessful) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (stats.isSuccessful) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (stats.isSuccessful) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (stats.isSuccessful) "Completed Successfully" else "Failed",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )

                        if(stats.isSuccessful){
                            Text(
                                text = "At ${stats.completedTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

fun calculateCurrentQuestStreak(questStats: Map<String, QuestStats>): Int {
    if (questStats.isEmpty()) return 0

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val sortedStats = questStats.mapKeys { LocalDate.parse(it.key, formatter) }
        .toSortedMap(reverseOrder()) // Go from latest to earliest

    var streak = 0
    var expectedDate: LocalDate? = null

    for ((date, stats) in sortedStats) {
        if (!stats.isSuccessful) break

        if (expectedDate == null) {
            // Start from the latest date
            expectedDate = date
            streak++
        } else {
            val expectedPrev = expectedDate.minusDays(1)
            if (date == expectedPrev) {
                streak++
                expectedDate = date
            } else {
                break // missed a day
            }
        }
    }

    return streak
}

fun calculateLongestQuestStreak(questStats: Map<String, QuestStats>): Int {
    if (questStats.isEmpty()) return 0

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val sortedStats = questStats.mapKeys { LocalDate.parse(it.key, formatter) }
        .toSortedMap()

    var longestStreak = 0
    var currentStreak = 0
    var prevDate: LocalDate? = null

    for ((date, stats) in sortedStats) {
        if (!stats.isSuccessful) {
            currentStreak = 0
            prevDate = null
            continue
        }

        if (prevDate == null) {
            currentStreak = 1
        } else {
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(prevDate, date)
            currentStreak = if (daysBetween == 1L) {
                currentStreak + 1
            } else {
                1 // streak broken, start new
            }
        }

        longestStreak = maxOf(longestStreak, currentStreak)
        prevDate = date
    }

    return longestStreak
}
fun calculateWeeklyAverageSuccess(questStats: Map<String, QuestStats>): Double {
    if (questStats.isEmpty()) return 0.0

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val weekMap = mutableMapOf<Pair<Int, Int>, MutableList<QuestStats>>() // Pair<Year, Week>

    for ((dateStr, stats) in questStats) {
        val date = LocalDate.parse(dateStr, formatter)
        val weekFields = WeekFields.of(Locale.getDefault())
        val weekOfYear = date.get(weekFields.weekOfWeekBasedYear())
        val year = date.get(weekFields.weekBasedYear())

        val key = year to weekOfYear
        weekMap.getOrPut(key) { mutableListOf() }.add(stats)
    }

    val weeklySuccessCounts = weekMap.values.map { week ->
        week.count { it.isSuccessful }
    }

    return if (weeklySuccessCounts.isEmpty()) 0.0
    else weeklySuccessCounts.average()
}
