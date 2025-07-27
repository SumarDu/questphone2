package neth.iecal.questphone.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.preferences.GestureSettingsRepository
import kotlin.math.abs
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import neth.iecal.questphone.R
import neth.iecal.questphone.utils.VibrationHelper
import neth.iecal.questphone.data.game.StreakCheckReturn
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.checkIfStreakFailed
import neth.iecal.questphone.data.game.continueStreak
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.focus.DeepFocus
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.screens.launcher.components.LiveClock
import neth.iecal.questphone.ui.screens.launcher.components.LiveTimer
import neth.iecal.questphone.ui.screens.quest.DialogState
import neth.iecal.questphone.ui.screens.quest.RewardDialogInfo
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.formatHour
import neth.iecal.questphone.utils.formatInstantToDate
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import neth.iecal.questphone.utils.getCurrentDay
import neth.iecal.questphone.utils.isSetToDefaultLauncher
import neth.iecal.questphone.utils.openDefaultLauncherSettings
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gestureRepo = remember { GestureSettingsRepository(context) }
    val swipeUpApp by gestureRepo.swipeUpApp.collectAsState(initial = null)

    val timerViewModel: TimerViewModel = viewModel()
    val timerText by timerViewModel.timerText.collectAsState()
    val timerMode by timerViewModel.timerMode.collectAsState()
    val timerState by TimerService.timerState.collectAsState()
    var showStartConfirmation by remember { mutableStateOf(false) }
    var selectedQuestForConfirmation by remember { mutableStateOf<CommonQuestInfo?>(null) }
    var showQuestFinishedDialog by remember { mutableStateOf(false) }
    var finishedQuestId by remember { mutableStateOf<String?>(null) }
    var showAddTimeDialog by remember { mutableStateOf(false) }
    var finishedQuestForReopening by remember { mutableStateOf<CommonQuestInfo?>(null) }

    LaunchedEffect(Unit) {
        timerViewModel.questFinishedEvent.collect { questId ->
            finishedQuestId = questId
            showQuestFinishedDialog = true
            // Store the finished quest for potential reopening
            scope.launch {
                val dao = QuestDatabaseProvider.getInstance(context).questDao()
                finishedQuestForReopening = dao.getQuestById(questId)
            }
        }
    }

    // Helper function to check if a quest is finished and can reopen dialog
    fun isQuestFinishedAndCanReopen(quest: CommonQuestInfo): Boolean {
        val now = System.currentTimeMillis()
        val questDurationMillis = TimeUnit.MINUTES.toMillis(quest.quest_duration_minutes.toLong())
        val questEndTime = quest.quest_started_at + questDurationMillis
        
        // Quest is finished if it was started and the time has passed
        return quest.quest_started_at > 0 && now > questEndTime && quest.last_completed_on != getCurrentDate()
    }

    // Helper function to check if there's an active or overdue quest that should block interactions
    fun hasActiveOrOverdueQuestBlocking(currentQuest: CommonQuestInfo, questList: List<CommonQuestInfo>): Boolean {
        val activeQuest = questList.find { it.quest_started_at > 0 && it.last_completed_on != getCurrentDate() }
        if (activeQuest == null) return false
        
        // Don't block if clicking on the same active quest
        if (activeQuest.id == currentQuest.id) return false
        
        val now = System.currentTimeMillis()
        val questDurationMillis = TimeUnit.MINUTES.toMillis(activeQuest.quest_duration_minutes.toLong())
        val questEndTime = activeQuest.quest_started_at + questDurationMillis
        
        // Block if quest is active or overdue
        return now <= questEndTime || (now > questEndTime && activeQuest.last_completed_on != getCurrentDate())
    }
    val swipeDownApp by gestureRepo.swipeDownApp.collectAsState(initial = null)
    val swipeLeftApp by gestureRepo.swipeLeftApp.collectAsState(initial = null)
    val swipeRightApp by gestureRepo.swipeRightApp.collectAsState(initial = null)

    val dao = QuestDatabaseProvider.getInstance(context).questDao()

    val questHelper = QuestHelper(context)
    val questListUnfiltered by dao.getAllQuests().collectAsState(initial = emptyList())
    val initial = remember { mutableStateOf(true) }
    val questList = remember { mutableStateListOf<CommonQuestInfo>() }

    val completedQuests = remember { SnapshotStateList<String>() }
    val progress = (completedQuests.size.toFloat() / questList.size.toFloat()).coerceIn(0f,1f)


    BackHandler {  }


    fun streakFailResultHandler(streakCheckReturn: StreakCheckReturn?){
        if(streakCheckReturn!=null){
            RewardDialogInfo.streakData = streakCheckReturn
            if(streakCheckReturn.streakFreezersUsed!=null){
                RewardDialogInfo.currentDialog = DialogState.STREAK_UP
            }
            if(streakCheckReturn.streakDaysLost!=null){
                RewardDialogInfo.currentDialog = DialogState.STREAK_FAILED
            }
            RewardDialogInfo.isRewardDialogVisible = true

        }
    }

    LaunchedEffect(questListUnfiltered) {
        if (initial.value) {
            initial.value = false // Ignore the first emission (initial = emptyList())
        } else {
            val todayDay = getCurrentDay()
            val isUserCreatedToday = getCurrentDate() == formatInstantToDate(User.userInfo.created_on)

            Log.d("IsUserCreatedToday",isUserCreatedToday.toString())
            val list = questListUnfiltered.filter {
                !it.is_destroyed && 
                (
                    // Regular quests: check if today is in selected_days
                    it.selected_days.contains(todayDay) ||
                    // Calendar quests: check if auto_destruct matches today's date
                    (it.calendar_event_id != null && it.auto_destruct == getCurrentDate())
                ) &&
                (isUserCreatedToday || it.created_on != getCurrentDate())
            }
            questList.clear()
            questList.addAll(list)


            questList.forEach { item ->
                if (item.last_completed_on == getCurrentDate()) {
                    completedQuests.add(item.title)
                }
                if (questHelper.isQuestRunning(item.title)) {
                    viewQuest(item, navController)
                }
            }

            val data = context.getSharedPreferences("onboard", MODE_PRIVATE)


            if (User.userInfo.streak.currentStreak != 0) {
                streakFailResultHandler(User.checkIfStreakFailed())
            }
            if (completedQuests.size == questList.size && data.getBoolean("onboard", false)) {
                if (User.continueStreak()) {
                    RewardDialogInfo.currentDialog = DialogState.STREAK_UP
                    RewardDialogInfo.isRewardDialogVisible = true
                }
            }

        }
    }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    var dragX = 0f
                    var dragY = 0f
                    do {
                        val ev = awaitPointerEvent()
                        val change = ev.changes.first()
                        dragX += change.positionChange().x
                        dragY += change.positionChange().y

                        val swipeThreshold = 50f

                        if (abs(dragX) > abs(dragY)) { // Horizontal swipe
                            if (dragX > swipeThreshold) { // Swipe Right
                                scope.launch {
                                    swipeRightApp?.let { pkg ->
                                        context.packageManager.getLaunchIntentForPackage(pkg)?.let { context.startActivity(it) }
                                    }
                                }
                                VibrationHelper.vibrate(30)
                                break
                            } else if (dragX < -swipeThreshold) { // Swipe Left
                                scope.launch {
                                    swipeLeftApp?.let { pkg ->
                                        context.packageManager.getLaunchIntentForPackage(pkg)?.let { context.startActivity(it) }
                                    }
                                }
                                VibrationHelper.vibrate(30)
                                break
                            }
                        } else { // Vertical swipe
                            if (dragY < -swipeThreshold) { // Swipe Up
                                scope.launch {
                                    val pkg = swipeUpApp
                                    if (pkg != null) {
                                        context.packageManager.getLaunchIntentForPackage(pkg)?.let { context.startActivity(it) }
                                    } else {
                                        navController.navigate(Screen.AppList.route)
                                    }
                                }
                                VibrationHelper.vibrate(30)
                                break
                            } else if (dragY > swipeThreshold) { // Swipe Down
                                scope.launch {
                                    swipeDownApp?.let { pkg ->
                                        context.packageManager.getLaunchIntentForPackage(pkg)?.let { context.startActivity(it) }
                                    }
                                }
                                VibrationHelper.vibrate(30)
                                break
                            }
                        }
                    } while (change.pressed)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coins display on the left
            Image(
                painter = painterResource(R.drawable.coin_icon),
                contentDescription = "Streak",
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
                    .clickable{
                        navController.navigate(Screen.Store.route)
                    }
            )
            Text(
                text = "${User.userInfo.coins}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.size(8.dp))

            Image(
                painter = painterResource(R.drawable.streak),
                contentDescription = "Streak",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
                    .clickable{
                        navController.navigate(Screen.Store.route)
                    }
            )
            Text(
                text = "${User.userInfo.streak.currentStreak}D",
                style = MaterialTheme.typography.bodyLarge,
            )


            Spacer(modifier = Modifier.weight(1f)) // Pushes the Icon to the right

            Icon(
                painter = painterResource(R.drawable.outline_store_24),
                contentDescription = "Shop",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
                    .clickable {
                    navController.navigate(Screen.Store.route)
                }
            )

            Icon(
                painter = painterResource(id = R.drawable.outline_progress_activity_24),
                contentDescription = "Stats",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
                    .clickable { navController.navigate(Screen.Stats.route) }
            )

            // Settings icon now replaces Profile
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
                    .clickable {
                        navController.navigate(Screen.Settings.route)
                    }
            )
        }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LiveClock(modifier = Modifier)
                LiveTimer()
            }

            // QUESTS header
//            Text(
//                text = "QUESTS",
//                style = MaterialTheme.typography.headlineLarge,
//                fontFamily = JetBrainsMonoFont,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(bottom = 16.dp)
//                    .align(Alignment.CenterHorizontally)
//            )



            if(questList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp)
                        .padding(bottom = 32.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(questList.size){ index ->
                    val baseQuest = questList[index]
                    val timeRange = "${formatHour(baseQuest.time_range[0])} - ${formatHour(baseQuest.time_range[1])} : "
                    val prefix = if(baseQuest.time_range[0]==0&&baseQuest.time_range[1]==24) "" else timeRange
                    val isOver = questHelper.isOver(baseQuest)
                    val isCompleted = completedQuests.contains(baseQuest.title)
                    val isActive = timerState.activeQuestId == baseQuest.id && (timerMode == TimerMode.QUEST_COUNTDOWN || timerMode == TimerMode.BREAK)
                    
                    // Check if quest is overdue (started but not completed today)
                    val isOverdue = baseQuest.quest_started_at > 0 && 
                        baseQuest.last_completed_on != getCurrentDate() && 
                        System.currentTimeMillis() > (baseQuest.quest_started_at + TimeUnit.MINUTES.toMillis(baseQuest.quest_duration_minutes.toLong()))
                    QuestItem(
                        text =  if(QuestHelper.Companion.isInTimeRange(baseQuest) && isOver) baseQuest.title else  prefix +  baseQuest.title,
                        isCompleted = isCompleted,
                        isFailed = isOver,
                        isActive = isActive,
                        isOverdue = isOverdue,
                        modifier = Modifier.clickable(enabled = timerMode != TimerMode.UNPLANNED_BREAK && timerMode != TimerMode.INFO && !(timerState.isDeepFocusLocking && baseQuest.id != timerState.activeQuestId)) {
                            // --- Quest Interaction Restrictions ---
                            
                            // Check if this quest is finished and can reopen the "Quest Finished" dialog
                            if (isQuestFinishedAndCanReopen(baseQuest)) {
                                finishedQuestId = baseQuest.id
                                finishedQuestForReopening = baseQuest
                                showQuestFinishedDialog = true
                                return@clickable
                            }
                            
                            // Check if there's an active or overdue quest blocking interactions
                            if (hasActiveOrOverdueQuestBlocking(baseQuest, questList)) {
                                // Don't allow clicking on other quests when there's an active/overdue quest
                                return@clickable
                            }

                            // --- Original Quest Locking Logic ---
                            // For completed quests, always open info screen directly
                            if (isCompleted) {
                                viewQuest(baseQuest, navController)
                                return@clickable
                            }
                            
                            // For active quests, always open info/statistics screen directly
                            if (isActive) {
                                navController.navigate(Screen.QuestStats.route + baseQuest.id)
                                return@clickable
                            }
                            
                            // 1. Standard SwiftMark check (only if not locked by Deep Focus)
                            if (baseQuest.integration_id == IntegrationId.SWIFT_MARK) {
                                val sharedPreferences = context.getSharedPreferences("swift_mark_prefs", MODE_PRIVATE)
                                val isConfirmed = sharedPreferences.getBoolean("is_confirmed_${baseQuest.id}", false)

                                if (!isConfirmed) {
                                    selectedQuestForConfirmation = baseQuest
                                    showStartConfirmation = true
                                } else {
                                    viewQuest(baseQuest, navController)
                                }
                            } else {
                                // For non-SwiftMark quests, clicking them directly opens the view without confirmation.
                                viewQuest(baseQuest, navController)
                            }
                        })
                }
                item {
                    QuestItem(
                        text = "Manage Quests",
                        modifier = Modifier.clickable(enabled = timerState.mode != TimerMode.INFO && !timerState.isDeepFocusLocking && !hasActiveOrOverdueQuestBlocking(CommonQuestInfo(), questList)) {
                            navController.navigate(Screen.ListAllQuest.route)
                        }
                    )
                }
                if(!isSetToDefaultLauncher(context)){
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openDefaultLauncherSettings(context) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_info_24),
                                    contentDescription = "Information",
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Text(
                                    text = "Set QuestPhone as your default launcher for the best experience",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward, // or chevron_right
                                    contentDescription = "Open settings",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                    }
                }
            }

        }
    }}

    if (showStartConfirmation) {
        AlertDialog(
            onDismissRequest = { showStartConfirmation = false },
            title = { Text("Start Quest") },
            text = { Text("Are you ready to begin?") },
            confirmButton = {
                Button(
                    onClick = {
                        showStartConfirmation = false
                        selectedQuestForConfirmation?.let {
                            scope.launch {
                                val quest = it.copy(quest_started_at = System.currentTimeMillis())
                                val dao = QuestDatabaseProvider.getInstance(context).questDao()
                                dao.upsertQuest(quest)
                            }
                        }
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                Button(onClick = { showStartConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQuestFinishedDialog) {
        AlertDialog(
            onDismissRequest = { showQuestFinishedDialog = false },
            title = { Text("Quest Finished") },
            text = {
                Text("The quest timer has ended. What would you like to do?")
            },
            confirmButton = {
                TextButton(onClick = { showQuestFinishedDialog = false }) {
                    Text("Later")
                }
            },
            dismissButton = {
                Column {
                    TextButton(
                        onClick = {
                            showQuestFinishedDialog = false
                            finishedQuestId?.let { navController.navigate(Screen.ViewQuest.route + it) }
                        }
                    ) {
                        Text("Complete Quest")
                    }
                    TextButton(
                        onClick = {
                            showQuestFinishedDialog = false
                            showAddTimeDialog = true
                        }
                    ) {
                        Text("Add Time")
                    }
                }
            }
        )
    }

    if (showAddTimeDialog) {
        val timeOptions = listOf(5, 10, 20, 30, 60)
        AlertDialog(
            onDismissRequest = { showAddTimeDialog = false },
            title = { Text("Add Extra Time") },
            text = {
                Column {
                    Text("Select how much time to add.")
                    timeOptions.forEach { minutes ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    finishedQuestId?.let { questId ->
                                        val dao = QuestDatabaseProvider.getInstance(context).questDao()
                                        val quest = dao.getAllQuests().first().find { q -> q.id == questId }
                                        quest?.let { currentQuest ->
                                            val now = System.currentTimeMillis()
                                            val questStartTime = currentQuest.quest_started_at
                                            val questDurationMillis = TimeUnit.MINUTES.toMillis(currentQuest.quest_duration_minutes.toLong())
                                            val questEndTime = questStartTime + questDurationMillis

                                            val overtimeMillis = if (now > questEndTime) {
                                                now - questEndTime
                                            } else {
                                                0L
                                            }

                                            val updatedQuest = currentQuest.copy(
                                                quest_duration_minutes = currentQuest.quest_duration_minutes + minutes,
                                                quest_started_at = currentQuest.quest_started_at + overtimeMillis
                                            )
                                            dao.upsertQuest(updatedQuest)
                                        }
                                    }
                                }
                                showAddTimeDialog = false
                            }
                        ) {
                            Text("$minutes minutes")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddTimeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun viewQuest(baseQuest: CommonQuestInfo, navController: NavController) {
    navController.navigate(Screen.ViewQuest.route + baseQuest.id)
}
@Composable
fun QuestItem(
    text: String,
    isCompleted: Boolean = false,
    isFailed: Boolean = false,
    isActive: Boolean = false,
    isOverdue: Boolean = false,
    modifier: Modifier
) {
    Text(
        text = text,
        style = if (isCompleted || isFailed) {
            MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
        } else {
            MaterialTheme.typography.bodyLarge
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = when {
            isFailed -> MaterialTheme.colorScheme.error
            isOverdue -> Color(0xFFFF6B35) // Orange color for overdue quests
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        fontWeight = if (isActive) FontWeight.Bold else null,
        textAlign = TextAlign.Center
    )
}
