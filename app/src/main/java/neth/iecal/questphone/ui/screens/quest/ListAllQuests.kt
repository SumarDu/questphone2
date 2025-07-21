package neth.iecal.questphone.ui.screens.quest

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.utils.getCurrentDate
import java.util.concurrent.TimeUnit

@Composable
fun ListAllQuests(navHostController: NavHostController) {
    val context = LocalContext.current
    val viewModel: ListAllQuestsViewModel = viewModel(
                factory = ListAllQuestsViewModelFactory(context.applicationContext as Application, QuestDatabaseProvider.getInstance(context).questDao())
    )

    val filteredQuestList by viewModel.filteredQuests.collectAsState(initial = emptyList())
    val filteredClonedQuestList by viewModel.filteredClonedQuests.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val questToDelete by viewModel.questToDelete.collectAsState()
    
    // Helper function to check if there's an active or overdue quest that should block interactions
    fun isQuestFinishedAndCanReopen(quest: CommonQuestInfo): Boolean {
        val now = System.currentTimeMillis()
        val questDurationMillis = TimeUnit.MINUTES.toMillis(quest.quest_duration_minutes.toLong())
        val questEndTime = quest.quest_started_at + questDurationMillis

        return quest.quest_started_at > 0 && now > questEndTime && quest.last_completed_on != getCurrentDate()
    }

    fun hasActiveOrOverdueQuestBlocking(currentQuest: CommonQuestInfo, allQuests: List<CommonQuestInfo>): Boolean {
        val activeQuest = allQuests.find { it.quest_started_at > 0 && it.last_completed_on != getCurrentDate() }
        if (activeQuest == null) return false
        
        // Don't block if clicking on the same active quest
        if (activeQuest.id == currentQuest.id) return false
        
        val now = System.currentTimeMillis()
        val questDurationMillis = TimeUnit.MINUTES.toMillis(activeQuest.quest_duration_minutes.toLong())
        val questEndTime = activeQuest.quest_started_at + questDurationMillis
        
        // Block if quest is active or overdue
        return now <= questEndTime || (now > questEndTime && activeQuest.last_completed_on != getCurrentDate())
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Button(
                onClick = { navHostController.navigate(Screen.AddNewQuest.route) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(text = "Add Quest")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->

        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
            ) {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("All Quests",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                }
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        label = { Text("Search Quests") },
                        placeholder = { Text("Type Quest Title...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )
                }
                items(filteredQuestList, key = { it.id }) { questBase ->
                    QuestItem(
                        quest = questBase,
                        onClick = {
                            val allQuests = filteredQuestList + filteredClonedQuestList
                            if (hasActiveOrOverdueQuestBlocking(questBase, allQuests)) {
                                return@QuestItem
                            }

                            // Always navigate to quest statistics/info screen
                            navHostController.navigate(Screen.QuestStats.route + questBase.id)
                        },
                        onDelete = { viewModel.onQuestDeleteRequest(questBase) },
                        onClone = { viewModel.onQuestCloneRequest(questBase) }
                    )
                }

                if (filteredClonedQuestList.isNotEmpty()) {
                    item {
                        Text("Cloned Quests",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    items(filteredClonedQuestList, key = { it.id }) { questBase ->
                        QuestItem(
                            quest = questBase,
                            onClick = {
                                val allQuests = filteredQuestList + filteredClonedQuestList
                                if (hasActiveOrOverdueQuestBlocking(questBase, allQuests)) {
                                    return@QuestItem
                                }

                                // Always navigate to quest statistics/info screen
                                navHostController.navigate(Screen.QuestStats.route + questBase.id)
                            },
                            onDelete = { viewModel.onQuestDeleteRequest(questBase) },
                            onClone = { viewModel.onQuestCloneRequest(questBase) }
                        )
                    }
                }
            }

            questToDelete?.let { quest ->
                AlertDialog(
                    onDismissRequest = { viewModel.onQuestDeleteCancel() },
                    title = { Text("Delete Quest") },
                    text = { Text("Are you sure you want to delete the quest \"${quest.title}\"? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onQuestDeleteConfirm() }) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onQuestDeleteCancel() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

}

@Composable
private fun QuestItem(
    quest: CommonQuestInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item preview/icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(quest.integration_id.icon),
                        contentDescription = quest.integration_id.name
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                                // Item details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = quest.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClone) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Clone quest")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete quest")
                        }
                    }
                    

                    if(quest.is_destroyed){
                        Text(
                            text = "Destroyed",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = if(quest.selected_days.size == 7) "Everyday" else quest.selected_days.joinToString(", ") { it.name },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                }

            }
        }

    }

}

