package neth.iecal.questphone.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import neth.iecal.questphone.MainActivity
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.QuestPriority
import neth.iecal.questphone.utils.SchedulingUtils
import neth.iecal.questphone.utils.formatTimeMinutes
import neth.iecal.questphone.utils.isAllDayRange
import neth.iecal.questphone.utils.toMinutesRange
import java.time.LocalDate

class QuestListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuestListWidget()
}

class QuestListWidgetRepository(
    private val context: Context
) {
    fun getTodayQuests(): Flow<List<CommonQuestInfo>> {
        val dao = QuestDatabaseProvider.getInstance(context).questDao()
        val today = LocalDate.now()
        
        return dao.getAllQuests().map { allQuests ->
            allQuests
                .filter { quest ->
                    !quest.is_destroyed &&
                    SchedulingUtils.isQuestAvailableOnDate(quest.scheduling_info, today)
                }
                .sortedWith(
                    compareBy(
                        { if (isAllDayRange(it.time_range)) 1 else 0 },
                        { toMinutesRange(it.time_range).first }
                    )
                )
        }
    }
}

class QuestListWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = QuestListWidgetRepository(context)

        provideContent {
            val quests by repo.getTodayQuests().collectAsState(initial = emptyList())

            GlanceTheme {
                QuestList(
                    context = context,
                    quests = quests
                )
            }
        }
    }

    @Composable
    fun QuestList(
        context: Context,
        quests: List<CommonQuestInfo>
    ) {
        Scaffold(
            modifier = GlanceModifier.fillMaxSize(),
            backgroundColor = GlanceTheme.colors.widgetBackground,
            titleBar = {
                Row(
                    modifier = GlanceModifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Today's Quests",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
                    )
                }
            }
        ) {
            if (quests.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No quests for today",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 16.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    items(quests, itemId = { it.id.hashCode().toLong() }) { quest ->
                        QuestWidgetTile(quest)
                        Spacer(GlanceModifier.height(8.dp))
                    }

                    item {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Open App",
                                modifier = GlanceModifier
                                    .clickable(actionStartActivity<MainActivity>())
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun QuestWidgetTile(quest: CommonQuestInfo) {
        val (startMin, endMin) = toMinutesRange(quest.time_range)
        
        val statusColor = when (quest.priority) {
            QuestPriority.IMPORTANT_URGENT -> androidx.glance.color.ColorProvider(
                day = androidx.compose.ui.graphics.Color(0xFFEF4444),
                night = androidx.compose.ui.graphics.Color(0xFFEF4444)
            )
            QuestPriority.IMPORTANT_NOT_URGENT -> androidx.glance.color.ColorProvider(
                day = androidx.compose.ui.graphics.Color(0xFF10B981),
                night = androidx.compose.ui.graphics.Color(0xFF10B981)
            )
            QuestPriority.NOT_IMPORTANT_URGENT -> androidx.glance.color.ColorProvider(
                day = androidx.compose.ui.graphics.Color(0xFFF5DEB3),
                night = androidx.compose.ui.graphics.Color(0xFFF5DEB3)
            )
            QuestPriority.STABLE -> androidx.glance.color.ColorProvider(
                day = androidx.compose.ui.graphics.Color(0xFF3B82F6),
                night = androidx.compose.ui.graphics.Color(0xFF3B82F6)
            )
            QuestPriority.NOT_IMPORTANT_NOT_URGENT -> androidx.glance.color.ColorProvider(
                day = androidx.compose.ui.graphics.Color(0xFFD1D5DB),
                night = androidx.compose.ui.graphics.Color(0xFFD1D5DB)
            )
        }

        val durationText = formatDuration(quest.quest_duration_minutes)
        val startText = if (isAllDayRange(quest.time_range)) null else formatTimeMinutes(startMin)
        val endText = if (isAllDayRange(quest.time_range)) null else formatTimeMinutes(endMin)
        val deadlineText = if (quest.deadline_minutes >= 0) formatTimeMinutes(quest.deadline_minutes) else null

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left colored bar
                Box(
                    modifier = GlanceModifier
                        .width(6.dp)
                        .height(44.dp)
                        .background(statusColor)
                        .cornerRadius(12.dp)
                ) { }
                
                Spacer(modifier = GlanceModifier.width(12.dp))
                
                // Title and info
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = quest.title,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 2
                    )
                    
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    
                    Row(
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = durationText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
                
                // Right times column
                Column(horizontalAlignment = Alignment.End) {
                    if (startText != null) {
                        Text(
                            text = startText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 12.sp
                            )
                        )
                    }
                    if (endText != null) {
                        Text(
                            text = endText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 12.sp
                            )
                        )
                    }
                    if (deadlineText != null) {
                        Text(
                            text = "⏰ $deadlineText",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(
                                    day = androidx.compose.ui.graphics.Color(0xFFEF4444),
                                    night = androidx.compose.ui.graphics.Color(0xFFEF4444)
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

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
}
