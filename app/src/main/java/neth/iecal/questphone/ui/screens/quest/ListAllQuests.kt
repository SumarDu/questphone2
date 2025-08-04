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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.SchedulingUtils
import neth.iecal.questphone.utils.CalendarPermissionHelper
import java.util.concurrent.TimeUnit
import androidx.compose.material.icons.filled.Sync
import android.app.Activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListAllQuests(navHostController: NavHostController) {
    val context = LocalContext.current
    val viewModel: ListAllQuestsViewModel = viewModel(
        factory = ListAllQuestsViewModelFactory(context.applicationContext as Application, QuestDatabaseProvider.getInstance(context).questDao())
    )

    val filteredQuestList by viewModel.filteredQuests.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isEditingEnabled by viewModel.isEditingEnabled.collectAsState(initial = true)
    val questToDelete by viewModel.questToDelete.collectAsState()

    fun hasActiveOrOverdueQuestBlocking(currentQuest: CommonQuestInfo, allQuests: List<CommonQuestInfo>): Boolean {
        val activeQuest = allQuests.find { it.quest_started_at > 0 && it.last_completed_on != getCurrentDate() }
        if (activeQuest == null) return false
        if (activeQuest.id == currentQuest.id) return false
        val now = System.currentTimeMillis()
        val questDurationMillis = TimeUnit.MINUTES.toMillis(activeQuest.quest_duration_minutes.toLong())
        val questEndTime = activeQuest.quest_started_at + questDurationMillis
        return now <= questEndTime || (now > questEndTime && activeQuest.last_completed_on != getCurrentDate())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("All Quests") },
                actions = {
                    IconButton(onClick = {
                        if (CalendarPermissionHelper.hasCalendarPermission(context)) {
                            viewModel.syncCalendar()
                        } else {
                            CalendarPermissionHelper.requestCalendarPermission(context as Activity)
                        }
                    }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Calendar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isEditingEnabled) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { navHostController.navigate(Screen.AddNewQuest.route) },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Quest")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    label = { Text("Search Quests") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            }

            TabRow(selectedTabIndex = selectedTab.ordinal) {
                QuestTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.size(12.dp)) } // Add space at the top
                items(filteredQuestList, key = { it.id }) { questBase ->
                    QuestItem(
                        quest = questBase,
                        onClick = {
                            if (hasActiveOrOverdueQuestBlocking(questBase, filteredQuestList)) {
                                return@QuestItem
                            }
                            navHostController.navigate(Screen.QuestStats.route + questBase.id)
                        },
                        onDelete = { viewModel.onQuestDeleteRequest(questBase) },
                        onClone = { viewModel.onQuestCloneRequest(questBase) },
                        onEdit = { 
                            val route = questBase.integration_id.name + "/" + questBase.id
                            navHostController.navigate(route)
                        },
                        isEditingEnabled = isEditingEnabled
                    )
                }
                item { Spacer(modifier = Modifier.size(12.dp)) } // Add space at the bottom
            }
        }

        if (questToDelete != null) {
            AlertDialog(
                onDismissRequest = { viewModel.onQuestDeleteCancel() },
                title = { Text("Delete Quest") },
                text = { Text("Are you sure you want to delete the quest \"${questToDelete!!.title}\"? This action cannot be undone.") },
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

@Composable
private fun QuestItem(
    quest: CommonQuestInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit,
    onEdit: () -> Unit,
    isEditingEnabled: Boolean
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
                        if (isEditingEnabled) {
                            IconButton(onClick = onClone) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Clone quest")
                            }
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit quest")
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete quest")
                            }
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
                        // Show scheduling information for quests
                        val displayText = SchedulingUtils.getSchedulingDescription(quest.scheduling_info)
                        
                        Text(
                            text = displayText,
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

