package neth.iecal.questphone.widget

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.utils.getCurrentDate
import java.util.UUID

/**
 * Dialog activity for selecting a quest to start from the widget
 */
class WidgetQuestSelectionDialogActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make this activity appear as a dialog over other apps
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        setContent {
            MaterialTheme {
                QuestSelectionDialog(
                    onQuestSelected = { quest ->
                        cloneAndStartQuest(quest)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
    
    private fun cloneAndStartQuest(quest: CommonQuestInfo) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            val questDao = QuestDatabaseProvider.getInstance(this@WidgetQuestSelectionDialogActivity).questDao()
            val today = getCurrentDate()
            val clonedQuestsCount = questDao.getClonedQuestsCountForToday(today, "${quest.title} [C%")
            val newTitle = "${quest.title} [C${clonedQuestsCount + 1}]"

            val clonedQuest = quest.copy(
                id = UUID.randomUUID().toString(),
                title = newTitle,
                auto_destruct = today,
                quest_started_at = System.currentTimeMillis(),
                last_completed_at = 0,
                last_completed_on = ""
            )
            questDao.upsertQuest(clonedQuest)
        }
    }
}

@Composable
fun QuestSelectionDialog(
    onQuestSelected: (CommonQuestInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val questDao = remember { QuestDatabaseProvider.getInstance(context).questDao() }
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState()
    
    var allQuests by remember { mutableStateOf<List<CommonQuestInfo>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        questDao.getPermanentQuests().collect { quests ->
            allQuests = quests
        }
    }
    
    // Helper functions
    fun isRepeatingQuest(quest: CommonQuestInfo): Boolean {
        return quest.selected_days.isNotEmpty()
    }
    
    fun isClonedQuest(quest: CommonQuestInfo): Boolean {
        return quest.title.contains("[C]") || quest.auto_destruct == getCurrentDate()
    }
    
    fun isOneTimeQuest(quest: CommonQuestInfo): Boolean {
        return !isRepeatingQuest(quest) && !isClonedQuest(quest)
    }
    
    // Filter quests based on settings
    val filteredQuests = allQuests.filter { quest ->
        when {
            isRepeatingQuest(quest) -> {
                // Check if showing repeating quests is enabled
                if (!settings.showRepeatingQuestsInDialog) {
                    false
                } else {
                    // If specific repeating quests are selected, only show those
                    if (settings.selectedRepeatingQuestIds.isEmpty()) {
                        true // Show all repeating quests
                    } else {
                        quest.id in settings.selectedRepeatingQuestIds
                    }
                }
            }
            isClonedQuest(quest) -> settings.showClonedQuestsInDialog
            isOneTimeQuest(quest) -> settings.showOneTimeQuestsInDialog
            else -> true
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a Quest") },
        text = {
            if (filteredQuests.isEmpty()) {
                Text(
                    text = "No quests available. Check your filter settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(filteredQuests) { quest ->
                        Text(
                            text = quest.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQuestSelected(quest) }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        )
                        Divider()
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
