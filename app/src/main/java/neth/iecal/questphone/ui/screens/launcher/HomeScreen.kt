package neth.iecal.questphone.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import java.time.LocalDate
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
import neth.iecal.questphone.utils.formatTimeMinutes
import neth.iecal.questphone.utils.toMinutesRange
import neth.iecal.questphone.utils.isAllDayRange
import neth.iecal.questphone.utils.formatInstantToDate
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import neth.iecal.questphone.utils.getCurrentDay
import neth.iecal.questphone.utils.isSetToDefaultLauncher
import neth.iecal.questphone.utils.openDefaultLauncherSettings
import kotlinx.serialization.json.Json
import neth.iecal.questphone.utils.SchedulingUtils

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    
    // Reactive coin balance state
    var showPenaltyDialog by remember { mutableStateOf(false) }

    if (showPenaltyDialog) {
        AlertDialog(
            onDismissRequest = { showPenaltyDialog = false },
            confirmButton = {
                TextButton(onClick = { showPenaltyDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPenaltyDialog = false
                    navController.navigate(Screen.Store.route)
                }) {
                    Text("Магазин")
                }
            },
            title = { Text("Штрафи під час прострочки") },
            text = {
                val count = timerState.overduePenaltyCount
                Column {
                    Text(text = "Кількість списань: $count")
                    if (timerMode != neth.iecal.questphone.data.timer.TimerMode.OVERTIME) {
                        Spacer(Modifier.size(8.dp))
                        Text(text = "Наразі прострочки немає.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        )
    }
    var coinBalance by remember { mutableStateOf(User.userInfo.coins) }
    
    // Update coin balance when timer state changes (to catch penalty deductions)
    LaunchedEffect(timerState) {
        coinBalance = User.userInfo.coins
    }
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
            val today = LocalDate.now()
            val isUserCreatedToday = getCurrentDate() == formatInstantToDate(User.userInfo.created_on)

            Log.d("IsUserCreatedToday", isUserCreatedToday.toString())
            val list = questListUnfiltered.filter {
                !it.is_destroyed &&
                        SchedulingUtils.isQuestAvailableOnDate(it.scheduling_info, today) &&
                        (it.calendar_event_id != null || isUserCreatedToday || it.created_on != getCurrentDate())
            }.toMutableList()
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
    // Sidebar visibility state and dimensions
    val sidebarWidth = 48.dp
    val edgeTapWidth = 36.dp
    var isSidebarVisible by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // Sidebar overlays content; no left gutter

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                        showPenaltyDialog = true
                    }
            )
            Text(
                text = "$coinBalance",
                style = MaterialTheme.typography.bodyLarge,
                color = if (timerMode == TimerMode.OVERTIME && timerState.hasPenaltyApplied) {
                    Color.Red
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
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
                .offset(y = (-32).dp),
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

            // Paginated list: 4 quests per page with snap paging
            val tileHeight = 72.dp
            val tileSpacing = 12.dp
            // Sort quests: non-all-day first by start minutes; all-day at the end
            val sortedQuests = questList.sortedWith(
                compareBy(
                    { if (isAllDayRange(it.time_range)) 1 else 0 },
                    { toMinutesRange(it.time_range).first }
                )
            )
            val pageCount = (sortedQuests.size + 3) / 4
            if (pageCount > 0) {
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                    initialPage = 0,
                    pageCount = { pageCount }
                )
                androidx.compose.foundation.pager.VerticalPager(
                    state = pagerState,
                    modifier = Modifier.height(tileHeight * 4 + tileSpacing * 3 + tileSpacing * 2),
                    pageSpacing = tileSpacing,
                    userScrollEnabled = pageCount > 1,
                ) { page ->
                    val start = page * 4
                    val end = minOf(start + 4, sortedQuests.size)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(tileSpacing)
                    ) {
                        for (index in start until end) {
                            val baseQuest = sortedQuests[index]
                    val (startMin, endMin) = toMinutesRange(baseQuest.time_range)
                    val timeRange = "${formatTimeMinutes(startMin)} - ${formatTimeMinutes(endMin)} : "
                    val prefix = if (isAllDayRange(baseQuest.time_range)) "" else timeRange
                    val isOver = questHelper.isOver(baseQuest)
                    val isCompleted = completedQuests.contains(baseQuest.title)
                    val isActive = timerState.activeQuestId == baseQuest.id && (timerMode == TimerMode.QUEST_COUNTDOWN || timerMode == TimerMode.BREAK)
                    
                    // Check if quest is overdue (started but not completed today)
                    val isOverdue = baseQuest.quest_started_at > 0 && 
                        baseQuest.last_completed_on != getCurrentDate() && 
                        System.currentTimeMillis() > (baseQuest.quest_started_at + TimeUnit.MINUTES.toMillis(baseQuest.quest_duration_minutes.toLong()))
                    run {
                        val durationText = formatDuration(baseQuest.quest_duration_minutes)
                        val timeText = if (isAllDayRange(baseQuest.time_range)) "All day" else "${formatTimeMinutes(startMin)} - ${formatTimeMinutes(endMin)}"
                        val subtitle = "$durationText • $timeText"
                        val statusColor = when {
                            isOver || isOverdue -> Color(0xFFEF4444) // red-500
                            isActive -> Color(0xFFF59E0B) // yellow-500
                            isCompleted -> Color(0xFF10B981) // green-500
                            else -> Color(0xFF10B981)
                        }

                        val onTileClick: () -> Unit = onTileClick@{
                            // --- Quest Interaction Restrictions ---
                            if (isQuestFinishedAndCanReopen(baseQuest)) {
                                finishedQuestId = baseQuest.id
                                finishedQuestForReopening = baseQuest
                                showQuestFinishedDialog = true
                                return@onTileClick
                            }
                            if (hasActiveOrOverdueQuestBlocking(baseQuest, questList)) {
                                return@onTileClick
                            }
                            // For completed quests, always open info screen directly
                            if (isCompleted) {
                                viewQuest(baseQuest, navController)
                                return@onTileClick
                            }
                            // For active quests, always open quest view screen directly
                            if (isActive) {
                                navController.navigate(Screen.ViewQuest.route + baseQuest.id)
                                return@onTileClick
                            }
                            // SwiftMark confirmation
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
                                viewQuest(baseQuest, navController)
                            }
                        }

                        val enabled = timerMode != TimerMode.UNPLANNED_BREAK &&
                                      timerMode != TimerMode.INFO &&
                                      timerMode != TimerMode.UNLOCK &&
                                      !(timerState.isDeepFocusLocking && baseQuest.id != timerState.activeQuestId)

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            QuestTile(
                                title = baseQuest.title,
                                subtitle = subtitle,
                                statusColor = statusColor,
                                enabled = enabled,
                                onClick = { if (enabled) onTileClick() },
                                onPlay = { if (enabled) onTileClick() },
                                modifier = Modifier
                                    .fillMaxWidth(0.94f)
                                    .height(tileHeight)
                            )
                        }
                    }
                    }
                }
                // Page indicator under the pager (e.g., 1/4), aligned to the right
                if (pageCount > 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/$pageCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
                if(!isSetToDefaultLauncher(context)){
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
        // Backdrop: close sidebar on single tap anywhere outside the sidebar
        if (isSidebarVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { isSidebarVisible = false }
                        )
                    }
            )
        }

        // Animated right sidebar (hidden by default, slides in on demand)
        androidx.compose.animation.AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = isSidebarVisible,
            enter = androidx.compose.animation.slideInHorizontally { fullWidth -> fullWidth },
            exit = androidx.compose.animation.slideOutHorizontally { fullWidth -> fullWidth }
        ) {
            Column(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SidebarButton(text = "TMRW") {
                    // TODO: Navigate to tomorrow's quests
                }
                Spacer(modifier = Modifier.height(8.dp))
                SidebarButton(text = "YDAY") {
                    // TODO: Navigate to yesterday's quests
                }
                Spacer(modifier = Modifier.height(8.dp))
                SidebarButton(icon = Icons.Default.DateRange) {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle case where no calendar app is found
                        Log.e("HomeScreen", "No calendar app found", e)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SidebarButton(icon = Icons.Default.List) {
                    if (timerState.mode != TimerMode.INFO && !timerState.isDeepFocusLocking && !hasActiveOrOverdueQuestBlocking(CommonQuestInfo(), questList)) {
                        navController.navigate(Screen.ListAllQuest.route)
                    }
                }
            }
        }

        // Right-edge double-tap trigger to toggle sidebar visibility (only when hidden)
        if (!isSidebarVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(edgeTapWidth)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                isSidebarVisible = true
                            }
                        )
                    }
            )
        }
    }
    }

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

@Composable
private fun QuestTile(
    title: String,
    subtitle: String,
    statusColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2937) // gray-800
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF) // gray-400
                    )
                }
            }
            IconButton(onClick = onPlay, enabled = enabled) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = "Play",
                    tint = Color(0xFF9CA3AF)
                )
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

@Composable
fun SidebarButton(
    text: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val buttonModifier = if (icon != null) {
        Modifier.size(40.dp) // Smaller square button for icons
    } else {
        Modifier.width(40.dp) // Narrower rectangular button for text
    }

    Box(
        modifier = buttonModifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                text.forEach { char ->
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Decorative icon
                modifier = Modifier.size(24.dp)
            )
        }
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
