package neth.iecal.questphone.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.preferences.GestureSettingsRepository
import kotlin.math.abs
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import neth.iecal.questphone.R
import neth.iecal.questphone.utils.VibrationHelper
import androidx.core.graphics.drawable.toBitmap
import neth.iecal.questphone.data.game.StreakCheckReturn
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.checkIfStreakFailed
import neth.iecal.questphone.data.game.continueStreak
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestPriority
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
import neth.iecal.questphone.utils.getPreviousDay
import neth.iecal.questphone.data.SchedulingInfo
import neth.iecal.questphone.data.SchedulingType
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import neth.iecal.questphone.utils.getCurrentDay
import neth.iecal.questphone.utils.isSetToDefaultLauncher
import neth.iecal.questphone.utils.openDefaultLauncherSettings
import kotlinx.serialization.json.Json
import neth.iecal.questphone.utils.json
import neth.iecal.questphone.utils.SchedulingUtils
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.data.local.PenaltyLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import neth.iecal.questphone.data.remote.DevModeManager
import neth.iecal.questphone.data.remote.SupabaseClient
import neth.iecal.questphone.data.remote.SupabaseSyncService
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

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

    // Initialize Supabase client with current mode
    LaunchedEffect(Unit) {
        try { SupabaseClient.init(context) } catch (_: Exception) {}
    }

    // Developer mode blocking flag
    val devSp = remember { context.getSharedPreferences("dev_mode", MODE_PRIVATE) }
    val pendingReinstall = remember { mutableStateOf(devSp.getBoolean("pending_reinstall", false)) }
    val devActive = remember { mutableStateOf(DevModeManager.isActive(context)) }
    var showModeDialog by remember { mutableStateOf(!DevModeManager.hasSelected(context)) }
    var showEndCommentDialog by remember { mutableStateOf(false) }
    var endCommentText by remember { mutableStateOf("") }
    
    // Reactive coin balance state
    var showPenaltyDialog by remember { mutableStateOf(false) }
    var penaltyLogs by remember { mutableStateOf<List<PenaltyLog>>(emptyList()) }

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
                LaunchedEffect(Unit) {
                    // Load recent penalty logs when dialog opens (off main thread)
                    penaltyLogs = withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(context).penaltyLogDao().getAll()
                    }
                }
                val df = remember { SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault()) }
                Column {
                    if (penaltyLogs.isEmpty()) {
                        Text(text = "Лог списань порожній.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    } else {
                        LazyColumn(Modifier.heightIn(max = 240.dp)) {
                            items(
                                penaltyLogs,
                                key = { it.id }
                            ) { log ->
                                val ts = df.format(Date(log.occurredAt))
                                val title = when (log.source) {
                                    "quest_sanction" -> "Санкція: ${log.questTitle ?: "Квест"}"
                                    else -> "Штраф (прострочка)"
                                }
                                Column(Modifier.padding(vertical = 6.dp)) {
                                    Text(title, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "−${log.amount} монет • ${ts}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // First-run mode selector
    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { /* force a choice */ },
            title = { Text("Select Mode") },
            text = { Text("Choose which backend to use. Developer Mode sends data to a separate dev Supabase project and shows a DEV banner.") },
            confirmButton = {
                TextButton(onClick = {
                    DevModeManager.setActive(context, true)
                    devActive.value = true
                    SupabaseClient.init(context)
                    Toast.makeText(context, "Developer Mode enabled", Toast.LENGTH_SHORT).show()
                    showModeDialog = false
                }) { Text("Developer Mode") }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Switch to Normal Mode first, then check Supabase for cp: dev_m_start
                    scope.launch {
                        DevModeManager.setActive(context, false)
                        devActive.value = false
                        SupabaseClient.init(context)
                        val needEnd = runCatching {
                            SupabaseSyncService(context).hasDevModeStartInLastTwo()
                        }.getOrDefault(false)
                        if (needEnd) {
                            endCommentText = ""
                            showEndCommentDialog = true
                        } else {
                            Toast.makeText(context, "Normal Mode selected", Toast.LENGTH_SHORT).show()
                        }
                        showModeDialog = false
                    }
                }) { Text("Normal Mode") }
            }
        )
    }

    // Forced end-comment dialog shown when selecting Normal Mode
    if (showEndCommentDialog) {
        AlertDialog(
            onDismissRequest = { /* forced dialog; do not allow dismiss without action */ },
            title = { Text("cp: dev_m_end comment") },
            text = {
                Column {
                    Text(text = "Enter a comment for the dev mode end checkpoint.")
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = endCommentText,
                        onValueChange = { endCommentText = it },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Switch to Normal Mode and run cleanup with the provided comment
                        DevModeManager.setActive(context, false)
                        devActive.value = false
                        SupabaseClient.init(context)
                        scope.launch {
                            runCatching {
                                SupabaseSyncService(context).checkAndCleanupDevModeArtifacts(endCommentText.ifBlank { null })
                            }
                        }
                        Toast.makeText(context, "Normal Mode selected", Toast.LENGTH_SHORT).show()
                        showEndCommentDialog = false
                    },
                    enabled = endCommentText.isNotBlank()
                ) { Text("Save") }
            }
        )
    }
    var coinBalance by remember { mutableStateOf(User.userInfo.coins) }
    var diamondBalance by remember { mutableStateOf(User.userInfo.diamonds) }
    
    // Update coin balance when timer state changes (to catch penalty deductions)
    LaunchedEffect(timerState) {
        coinBalance = User.userInfo.coins
        diamondBalance = User.userInfo.diamonds
    }
    var showStartConfirmation by remember { mutableStateOf(false) }
    // Diamonds breakdown dialog
    var showDiamondDialog by remember { mutableStateOf(false) }
    var selectedQuestForConfirmation by remember { mutableStateOf<CommonQuestInfo?>(null) }
    var showQuestFinishedDialog by remember { mutableStateOf(false) }
    var finishedQuestId by remember { mutableStateOf<String?>(null) }
    var showAddTimeDialog by remember { mutableStateOf(false) }
    var finishedQuestForReopening by remember { mutableStateOf<CommonQuestInfo?>(null) }
    
    // SwiftMark quick creation dialog state
    var showSwiftMarkDialog by remember { mutableStateOf(false) }
    var swiftMarkTitle by remember { mutableStateOf("") }
    var swiftMarkDuration by remember { mutableStateOf(10) }
    var showDurationDropdown by remember { mutableStateOf(false) }
    var swiftMarkBreak by remember { mutableStateOf(5) }
    var showBreakDurationDropdown by remember { mutableStateOf(false) }
    var swiftMarkStartMinutes by remember { mutableStateOf(0) }
    var swiftMarkEndMinutes by remember { mutableStateOf(1440) }
    var showTimeRangeDialog by remember { mutableStateOf(false) }

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

    // Blocking dialog if developer mode requires reinstall
    if (pendingReinstall.value) {
        AlertDialog(
            onDismissRequest = { /* non-dismissible */ },
            title = { Text("Developer Mode Required") },
            text = {
                Text(
                    "Please reinstall the app and switch to Developer Mode. A backup has been created. After reinstall, import it if needed."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // Keep the flag; exit app so user can reinstall
                    val activity = (context as? android.app.Activity)
                    activity?.finishAffinity()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {}
        )
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
    // Treat cloned/unplanned quests (e.g., "[C1]") as non-counting for overall progress
    fun isClonedQuestProgressExcluded(quest: CommonQuestInfo): Boolean {
        val clonedTitle = Regex("\\[C\\d+\\]").containsMatchIn(quest.title)
        return clonedTitle || quest.auto_destruct == getCurrentDate()
    }
    
    // Check if quest was started yesterday and completed today (overnight quest)
    fun isOvernightQuest(quest: CommonQuestInfo): Boolean {
        if (quest.quest_started_at <= 0) return false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startedDate = sdf.format(Date(quest.quest_started_at))
        val wasStartedYesterday = startedDate == getPreviousDay()
        val completedToday = quest.last_completed_on == getCurrentDate()
        return wasStartedYesterday && completedToday
    }
    val swipeDownApp by gestureRepo.swipeDownApp.collectAsState(initial = null)
    val swipeLeftApp by gestureRepo.swipeLeftApp.collectAsState(initial = null)
    val swipeRightApp by gestureRepo.swipeRightApp.collectAsState(initial = null)
    // New gestures
    val twoFingerSwipeUpApp by gestureRepo.twoFingerSwipeUpApp.collectAsState(initial = null)
    val twoFingerSwipeDownApp by gestureRepo.twoFingerSwipeDownApp.collectAsState(initial = null)
    val doubleTapBottomLeftApp by gestureRepo.doubleTapBottomLeftApp.collectAsState(initial = null)
    val doubleTapBottomRightApp by gestureRepo.doubleTapBottomRightApp.collectAsState(initial = null)
    val longPressApp by gestureRepo.longPressApp.collectAsState(initial = null)
    val edgeLeftSwipeUpApp by gestureRepo.edgeLeftSwipeUpApp.collectAsState(initial = null)
    val edgeLeftSwipeDownApp by gestureRepo.edgeLeftSwipeDownApp.collectAsState(initial = null)
    val edgeRightSwipeUpApp by gestureRepo.edgeRightSwipeUpApp.collectAsState(initial = null)
    val edgeRightSwipeDownApp by gestureRepo.edgeRightSwipeDownApp.collectAsState(initial = null)
    // Bottom quick applet settings
    val bottomRightMode by gestureRepo.doubleTapBottomRightMode.collectAsState(initial = "single")
    val bottomAppletApps by gestureRepo.bottomAppletApps.collectAsState(initial = emptyList())
    var showBottomApplet by remember { mutableStateOf(false) }

    val dao = QuestDatabaseProvider.getInstance(context).questDao()

    val questHelper = QuestHelper(context)
    val questListUnfiltered by dao.getAllQuests().collectAsState(initial = emptyList())
    val initial = remember { mutableStateOf(true) }
    val questList = remember { mutableStateListOf<CommonQuestInfo>() }

    val completedQuests = remember { SnapshotStateList<String>() }
    val nonClonedTotal = questList.count { !isClonedQuestProgressExcluded(it) }
    val progress = if (nonClonedTotal == 0) 0f else (completedQuests.size.toFloat() / nonClonedTotal.toFloat()).coerceIn(0f,1f)


    BackHandler {  }

    // Launch app intent without blocking UI thread
    fun launchPackage(pkg: String) {
        scope.launch {
            val intent = withContext(Dispatchers.IO) { context.packageManager.getLaunchIntentForPackage(pkg) }
            intent?.let {
                try { context.startActivity(it) } catch (_: Exception) {}
            }
        }
    }


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

            val list = questListUnfiltered.filter {
                !it.is_destroyed &&
                        SchedulingUtils.isQuestAvailableOnDate(it.scheduling_info, today)
            }.toMutableList()
            questList.clear()
            questList.addAll(list)

            // Recompute completed quests fresh each time to avoid duplicates and ensure correctness
            completedQuests.clear()
            questList.forEach { item ->
                if (item.last_completed_on == getCurrentDate() && !isClonedQuestProgressExcluded(item)) {
                    // Don't mark overnight quests as completed - they should be available to start again today
                    if (!isOvernightQuest(item)) {
                        completedQuests.add(item.title)
                    }
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
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val edgeTapWidthPx = with(density) { edgeTapWidth.toPx() }
    val sidebarWidthPx = with(density) { sidebarWidth.toPx() }
    val cornerMarginPx = with(density) { 96.dp.toPx() }

    // Snackbar host for floating sort change notifications
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
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
                .onSizeChanged { containerSize = it }
                // Tap/long-press/double-tap handler
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            longPressApp?.let { pkg ->
                                launchPackage(pkg)
                                VibrationHelper.vibrate(30)
                            }
                        },
                        onDoubleTap = { pos: Offset ->
                            val w = containerSize.width.toFloat()
                            val h = containerSize.height.toFloat()
                            if (h > 0 && w > 0 && pos.y > h - cornerMarginPx) {
                                when {
                                    pos.x < cornerMarginPx -> {
                                        doubleTapBottomLeftApp?.let { pkg ->
                                            launchPackage(pkg)
                                            VibrationHelper.vibrate(30)
                                        }
                                    }
                                    pos.x > w - cornerMarginPx -> {
                                        if (bottomRightMode == "applet") {
                                            showBottomApplet = true
                                            VibrationHelper.vibrate(15)
                                        } else {
                                            doubleTapBottomRightApp?.let { pkg ->
                                                launchPackage(pkg)
                                                VibrationHelper.vibrate(30)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown()
                    val startX = firstDown.position.x
                    val startY = firstDown.position.y
                    val isLeftEdge = startX < edgeTapWidthPx
                    // Reserve the outermost right gutter (sidebar drag area). Allow right-edge swipe just to the left of it.
                    val isRightEdge = if (containerSize.width > 0) {
                        val w = containerSize.width.toFloat()
                        startX in (w - sidebarWidthPx - edgeTapWidthPx)..(w - sidebarWidthPx)
                    } else false
                    var dragX = 0f
                    var dragY = 0f
                    var twoFingerDragY = 0f
                    do {
                        val ev = awaitPointerEvent()
                        val change = ev.changes.first()
                        dragX += change.positionChange().x
                        dragY += change.positionChange().y

                        val swipeThreshold = 50f
                        val edgeSwipeThreshold = 40f

                        // Two-finger vertical swipe detection
                        if (ev.changes.size >= 2) {
                            val avgDy = (ev.changes.map { it.positionChange().y }.average()).toFloat()
                            twoFingerDragY += avgDy
                            if (twoFingerDragY < -swipeThreshold) {
                                twoFingerSwipeUpApp?.let { pkg -> launchPackage(pkg) }
                                VibrationHelper.vibrate(30)
                                break
                            } else if (twoFingerDragY > swipeThreshold) {
                                twoFingerSwipeDownApp?.let { pkg -> launchPackage(pkg) }
                                VibrationHelper.vibrate(30)
                                break
                            }
                        }

                        if (abs(dragX) > abs(dragY)) { // Horizontal swipe
                            if (dragX > swipeThreshold) { // Swipe Right
                                swipeRightApp?.let { pkg -> launchPackage(pkg) }
                                VibrationHelper.vibrate(30)
                                break
                            } else if (dragX < -swipeThreshold) { // Swipe Left
                                swipeLeftApp?.let { pkg -> launchPackage(pkg) }
                                VibrationHelper.vibrate(30)
                                break
                            }
                        } else { // Vertical swipe
                            // Edge vertical swipes take precedence to avoid conflicts with generic swipes
                            if (isLeftEdge) {
                                if (dragY < -edgeSwipeThreshold) { // Left Edge Swipe Up
                                    edgeLeftSwipeUpApp?.let { pkg -> launchPackage(pkg) }
                                    VibrationHelper.vibrate(30)
                                    break
                                } else if (dragY > edgeSwipeThreshold) { // Left Edge Swipe Down
                                    edgeLeftSwipeDownApp?.let { pkg -> launchPackage(pkg) }
                                    VibrationHelper.vibrate(30)
                                    break
                                }
                            } else if (isRightEdge) {
                                if (dragY < -edgeSwipeThreshold) { // Right Edge Swipe Up
                                    edgeRightSwipeUpApp?.let { pkg -> launchPackage(pkg) }
                                    VibrationHelper.vibrate(30)
                                    break
                                } else if (dragY > edgeSwipeThreshold) { // Right Edge Swipe Down
                                    edgeRightSwipeDownApp?.let { pkg -> launchPackage(pkg) }
                                    VibrationHelper.vibrate(30)
                                    break
                                }
                            }

                            // Generic vertical swipes (non-edge)
                            if (dragY < -swipeThreshold) { // Swipe Up
                                val pkg = swipeUpApp
                                if (pkg != null) {
                                    launchPackage(pkg)
                                } else {
                                    navController.navigate(Screen.AppList.route)
                                }
                                VibrationHelper.vibrate(30)
                                break
                            } else if (dragY > swipeThreshold) { // Swipe Down
                                swipeDownApp?.let { pkg -> launchPackage(pkg) }
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

            // Diamonds breakdown dialog content
            if (showDiamondDialog) {
                AlertDialog(
                    onDismissRequest = { showDiamondDialog = false },
                    title = { Text("Diamonds") },
                    text = {
                        Column {
                            Text(text = "Available: ${User.userInfo.diamonds}", style = MaterialTheme.typography.bodyLarge)
                            Text(text = "Pending: ${User.userInfo.diamonds_pending}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Pending diamonds will become available the next day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDiamondDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
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

            // Diamonds display
            Image(
                painter = painterResource(R.drawable.diamond_icon),
                contentDescription = "Diamonds",
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
                    .clickable { showDiamondDialog = true }
            )
            Text(
                text = "${User.userInfo.diamonds + User.userInfo.diamonds_pending}",
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

            // Developer mode badge
            if (devActive.value) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "DEV",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFD32F2F))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

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

            // Sorting toggle state for main list (toggled by clicking the progress strip)
            var sortByPriority by remember { mutableStateOf(false) }

            if(questList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp)
                        .padding(bottom = 32.dp)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            sortByPriority = !sortByPriority
                            // Show floating notification about sorting mode change
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (sortByPriority) "Сортування: пріоритет" else "Сортування: час"
                                )
                            }
                        }
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(50)),
                    )
                }
            }

            // Paginated list: 4 quests per page with snap paging
            val tileHeight = 72.dp
            val tileSpacing = 12.dp
            // Sort quests: default by time (non-all-day first), or by priority when toggled via progress strip
            val sortedQuests = if (sortByPriority) {
                val byPriority = { q: CommonQuestInfo ->
                    when (q.priority) {
                        QuestPriority.IMPORTANT_URGENT -> 0
                        QuestPriority.IMPORTANT_NOT_URGENT -> 1
                        QuestPriority.NOT_IMPORTANT_URGENT -> 2
                        QuestPriority.STABLE -> 3
                        QuestPriority.NOT_IMPORTANT_NOT_URGENT -> 4
                    }
                }
                questList.sortedWith(
                    compareBy(
                        byPriority,
                        { if (isAllDayRange(it.time_range)) 1 else 0 },
                        { toMinutesRange(it.time_range).first }
                    )
                )
            } else {
                questList.sortedWith(
                    compareBy(
                        { if (isAllDayRange(it.time_range)) 1 else 0 },
                        { toMinutesRange(it.time_range).first }
                    )
                )
            }
            // Exclude finished quests from display (unless it's the active quest on break or overnight quest)
            val displayedQuests = sortedQuests.filter { q ->
                val completed = completedQuests.contains(q.title)
                val onBreak = (timerMode == TimerMode.BREAK && timerState.activeQuestId == q.id)
                
                // Keep overnight quests visible if they're scheduled for today
                // (they're already in questList, which means they passed the scheduling check)
                if (completed && isOvernightQuest(q)) {
                    true // Keep overnight quest visible
                } else {
                    !(completed && !onBreak) // Normal logic: hide if completed and not on break
                }
            }
            val pageCount = (displayedQuests.size + 3) / 4
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
                    val end = minOf(start + 4, displayedQuests.size)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(tileSpacing)
                    ) {
                        for (index in start until end) {
                            val baseQuest = displayedQuests[index]
                    val (startMin, endMin) = toMinutesRange(baseQuest.time_range)
                    val timeRange = "${formatTimeMinutes(startMin)} - ${formatTimeMinutes(endMin)} : "
                    val prefix = if (isAllDayRange(baseQuest.time_range)) "" else timeRange
                    val isOver = questHelper.isOver(baseQuest)
                    val isCompleted = completedQuests.contains(baseQuest.title)
                    val isActive = timerState.activeQuestId == baseQuest.id && (
                        timerMode == TimerMode.QUEST_COUNTDOWN ||
                        timerMode == TimerMode.BREAK ||
                        timerMode == TimerMode.UNPLANNED_BREAK
                    )
                    
                    // Check if quest is overdue (started but not completed today)
                    val isOverdue = baseQuest.quest_started_at > 0 && 
                        baseQuest.last_completed_on != getCurrentDate() && 
                        System.currentTimeMillis() > (baseQuest.quest_started_at + TimeUnit.MINUTES.toMillis(baseQuest.quest_duration_minutes.toLong()))
                    run {
                        // Priority strip color should reflect only priority, not state
                        val statusColor = when (baseQuest.priority) {
                            QuestPriority.IMPORTANT_URGENT -> Color(0xFFEF4444) // red
                            QuestPriority.IMPORTANT_NOT_URGENT -> Color(0xFF10B981) // green
                            QuestPriority.NOT_IMPORTANT_URGENT -> Color(0xFFF5DEB3) // beige
                            // STABLE adopts light gray color previously used by NOT_IMPORTANT_NOT_URGENT
                            QuestPriority.STABLE -> Color(0xFFD1D5DB) // light gray
                            // NOT_IMPORTANT_NOT_URGENT will be border-only; keep transparent fill
                            QuestPriority.NOT_IMPORTANT_NOT_URGENT -> Color.Transparent
                        }
                        val statusBorderColor: Color? = when (baseQuest.priority) {
                            QuestPriority.NOT_IMPORTANT_NOT_URGENT -> Color(0xFFD1D5DB) // light gray border
                            else -> null
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
                            // If quest deadline is missed, open info screen only (no start flow)
                            if (isOver) {
                                viewQuest(baseQuest, navController)
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

                        // --- Compute values for QuestTile ---
                        val durationText = if (baseQuest.integration_id == IntegrationId.DEEP_FOCUS) {
                            try {
                                val deepFocus = json.decodeFromString<neth.iecal.questphone.data.quest.focus.DeepFocus>(baseQuest.quest_json)
                                val regularSessions = deepFocus.minWorkSessions
                                val extraSessions = deepFocus.maxWorkSessions - deepFocus.minWorkSessions
                                val sessionMinutes = if (deepFocus.nextFocusDurationInMillis > 0) {
                                    TimeUnit.MILLISECONDS.toMinutes(deepFocus.nextFocusDurationInMillis).toInt()
                                } else {
                                    deepFocus.focusTimeConfig.initialTime.toIntOrNull() ?: 1
                                }
                                val sessionDuration = formatDuration(sessionMinutes)
                                "$regularSessions;$extraSessions * $sessionDuration"
                            } catch (e: Exception) {
                                formatDuration(baseQuest.quest_duration_minutes)
                            }
                        } else {
                            formatDuration(baseQuest.quest_duration_minutes)
                        }
                        val startText = if (isAllDayRange(baseQuest.time_range)) null else formatTimeMinutes(startMin)
                        val endText = if (isAllDayRange(baseQuest.time_range)) null else formatTimeMinutes(endMin)
                        val deadlineTextOnly = if (baseQuest.deadline_minutes >= 0) formatTimeMinutes(baseQuest.deadline_minutes) else null

                        // Background highlight per state
                        // If break is overdue (timerMode == OVERTIME), do NOT highlight tiles
                        val containerColor = when {
                            timerMode == TimerMode.OVERTIME -> Color(0xFF1F2937) // default gray-800 (no highlight)
                            isOver || isOverdue -> Color(0x33EF4444) // translucent red for overdue
                            isActive && (timerMode == TimerMode.BREAK || timerMode == TimerMode.UNPLANNED_BREAK) -> Color(0x3310B981) // translucent green for (un)planned break
                            isActive -> Color(0x33F59E0B) // translucent yellow for active
                            else -> Color(0xFF1F2937) // default gray-800
                        }

                        // Subtask progress based on instructions with '@' and saved checkbox states
                        val instructionsText = baseQuest.instructions ?: ""
                        val lines = instructionsText.split("\n")
                        var totalSubtasks = 0
                        var doneSubtasks = 0

                        val sp = context.getSharedPreferences("quest_checkboxes", MODE_PRIVATE)
                        val todayKey = "${baseQuest.id}_${getCurrentDate()}"
                        val savedStatesStr = sp.getString(todayKey, "") ?: ""
                        val savedMap = mutableMapOf<String, Boolean>()
                        if (savedStatesStr.isNotEmpty()) {
                            savedStatesStr.split(",").forEach { entry ->
                                val parts = entry.split(":")
                                if (parts.size == 2) {
                                    savedMap[parts[0]] = parts[1].toBoolean()
                                }
                            }
                        }

                        lines.forEachIndexed { idx, rawLine ->
                            if (rawLine.trim().startsWith("@")) {
                                totalSubtasks++
                                if (savedMap["checkbox_$idx"] == true) doneSubtasks++
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            QuestTile(
                                title = baseQuest.title,
                                duration = durationText,
                                subtaskProgress = if (totalSubtasks > 0) "${doneSubtasks}/${totalSubtasks}" else null,
                                coinReward = when {
                                    (baseQuest.reward_min > 0 || baseQuest.reward_max > 0) -> if (baseQuest.reward_min == baseQuest.reward_max) "${baseQuest.reward_min}" else "${baseQuest.reward_min}-${baseQuest.reward_max}"
                                    else -> null
                                },
                                hasCalendarMark = baseQuest.calendar_event_id != null,
                                startTime = startText,
                                endTime = endText,
                                deadlineTime = deadlineTextOnly,
                                containerColor = containerColor,
                                statusColor = statusColor,
                                statusBorderColor = statusBorderColor,
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
                SidebarButton(text = "WKEND") {
                    navController.navigate(Screen.DayQuests.route + "WKEND")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SidebarButton(text = "TMRW") {
                    navController.navigate(Screen.DayQuests.route + "TMRW")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SidebarButton(text = "TODAY") {
                    navController.navigate(Screen.DayQuests.route + "TODAY")
                }
                Spacer(modifier = Modifier.height(8.dp))
                SidebarButton(text = "YDAY") {
                    navController.navigate(Screen.DayQuests.route + "YDAY")
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
                Spacer(modifier = Modifier.height(8.dp))
                SidebarButton(icon = Icons.Default.Add) {
                    showSwiftMarkDialog = true
                    swiftMarkTitle = ""
                    swiftMarkDuration = 10
                    swiftMarkBreak = 5
                    swiftMarkStartMinutes = 0
                    swiftMarkEndMinutes = 1440
                }
            }
        }

        // Bottom-right double-tap: launch app or open quick applet
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(140.dp)
                .height(140.dp)
                .pointerInput(bottomRightMode, doubleTapBottomRightApp, bottomAppletApps) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (bottomRightMode == "applet") {
                                if (bottomAppletApps.isNotEmpty()) {
                                    VibrationHelper.vibrate(15)
                                    showBottomApplet = true
                                }
                            } else {
                                doubleTapBottomRightApp?.let { pkg ->
                                    context.packageManager.getLaunchIntentForPackage(pkg)?.let { intent ->
                                        VibrationHelper.vibrate(15)
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        }
                    )
                }
        )

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
        // Bottom Quick Applet overlay
        if (showBottomApplet) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { showBottomApplet = false }
            )
        }
        AnimatedVisibility(
            visible = showBottomApplet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val packages = bottomAppletApps.take(6)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    packages.forEach { pkg ->
                        val icon = remember(pkg) {
                            try { context.packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    launchPackage(pkg)
                                    VibrationHelper.vibrate(20)
                                    showBottomApplet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val iconBitmap = remember(icon) { icon?.toBitmap()?.asImageBitmap() }
                            iconBitmap?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }
                    }
                }
            }
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
    
    // SwiftMark quick creation dialog
    if (showSwiftMarkDialog) {
        val durationOptions = listOf(
            10 to "10 хв",
            20 to "20 хв",
            30 to "30 хв",
            60 to "60 хв",
            120 to "2 год"
        )
        
        AlertDialog(
            onDismissRequest = { showSwiftMarkDialog = false },
            title = { Text("Створити SwiftMark") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Швидкий тимчасовий квест без нагороди",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = swiftMarkTitle,
                        onValueChange = { swiftMarkTitle = it },
                        label = { Text("Назва") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDurationDropdown = true }
                    ) {
                        OutlinedTextField(
                            value = durationOptions.find { it.first == swiftMarkDuration }?.second ?: "",
                            onValueChange = { },
                            label = { Text("Час") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        
                        DropdownMenu(
                            expanded = showDurationDropdown,
                            onDismissRequest = { showDurationDropdown = false }
                        ) {
                            durationOptions.forEach { (minutes, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        swiftMarkDuration = minutes
                                        showDurationDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Break selector
                    val breakOptions = listOf(
                        0 to "Без перерви",
                        5 to "Перерва 5 хв",
                        10 to "Перерва 10 хв",
                        15 to "Перерва 15 хв"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBreakDurationDropdown = true }
                    ) {
                        OutlinedTextField(
                            value = breakOptions.find { it.first == swiftMarkBreak }?.second ?: "",
                            onValueChange = { },
                            label = { Text("Перерва") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        DropdownMenu(
                            expanded = showBreakDurationDropdown,
                            onDismissRequest = { showBreakDurationDropdown = false }
                        ) {
                            breakOptions.forEach { (minutes, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        swiftMarkBreak = minutes
                                        showBreakDurationDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Time range selector (uses existing dialog component)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimeRangeDialog = true }
                    ) {
                        OutlinedTextField(
                            value = "${formatTimeMinutes(swiftMarkStartMinutes)} — ${formatTimeMinutes(swiftMarkEndMinutes)}",
                            onValueChange = {},
                            label = { Text("Часовий діапазон") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (swiftMarkTitle.isNotBlank()) {
                            scope.launch {
                                val scheduling = SchedulingInfo(
                                    type = SchedulingType.SPECIFIC_DATE,
                                    selectedDays = emptySet(),
                                    specificDate = getCurrentDate()
                                )
                                val newQuest = CommonQuestInfo(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = swiftMarkTitle,
                                    reward_min = 0,
                                    reward_max = 0,
                                    integration_id = IntegrationId.SWIFT_MARK,
                                    priority = QuestPriority.NOT_IMPORTANT_URGENT,
                                    quest_duration_minutes = swiftMarkDuration,
                                    break_duration_minutes = swiftMarkBreak,
                                    quest_started_at = 0L,
                                    scheduling_info = scheduling,
                                    auto_destruct = SchedulingUtils.getExpirationDate(scheduling, getCurrentDate()),
                                    time_range = listOf(swiftMarkStartMinutes, swiftMarkEndMinutes),
                                    selected_days = emptySet(),
                                    synced = false,
                                    last_updated = System.currentTimeMillis()
                                )
                                dao.upsertQuest(newQuest)
                                showSwiftMarkDialog = false
                                Toast.makeText(context, "SwiftMark створено", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = swiftMarkTitle.isNotBlank()
                ) {
                    Text("Створити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwiftMarkDialog = false }) {
                    Text("Відмінити")
                }
            }
        )
    }

    // Time range picker dialog for SwiftMark
    if (showTimeRangeDialog) {
        neth.iecal.questphone.ui.screens.quest.setup.components.TimeRangeDialog(
            initialStartMinutes = swiftMarkStartMinutes,
            initialEndMinutes = swiftMarkEndMinutes,
            onDismiss = { showTimeRangeDialog = false },
            onConfirm = { s, e ->
                swiftMarkStartMinutes = s
                swiftMarkEndMinutes = e
                showTimeRangeDialog = false
            }
        )
    }
}

@Composable
fun QuestTile(
    title: String,
    duration: String,
    subtaskProgress: String?,
    coinReward: String? = null,
    hasCalendarMark: Boolean,
    startTime: String?,
    endTime: String?,
    deadlineTime: String?,
    containerColor: Color = Color(0xFF1F2937),
    statusColor: Color,
    statusBorderColor: Color? = null,
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
            containerColor = containerColor // highlight or default
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Left colored bar
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (statusBorderColor != null) {
                                Modifier.border(1.dp, statusBorderColor, RoundedCornerShape(12.dp))
                            } else {
                                Modifier.background(statusColor)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Title + chips
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coin reward chip (yellow) if present
                        if (coinReward != null) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = coinReward,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF111827)
                                )
                            }
                        }
                        // Duration chip
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFF374151), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = duration,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                        // Subtask progress chip
                        if (subtaskProgress != null) {
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF374151), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = subtaskProgress,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                        // Calendar marker
                        if (hasCalendarMark) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            // Right times column
            Column(horizontalAlignment = Alignment.End) {
                if (startTime != null) {
                    Text(
                        text = startTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
                if (endTime != null) {
                    Text(
                        text = endTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
                if (deadlineTime != null) {
                    Text(
                        text = deadlineTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF4444)
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
