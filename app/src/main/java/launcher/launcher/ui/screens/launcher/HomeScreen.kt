package launcher.launcher.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import android.util.Log
import launcher.launcher.R
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.launcher.components.LiveClock
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.getCurrentDate

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import launcher.launcher.data.game.StreakCheckReturn
import launcher.launcher.data.game.User
import launcher.launcher.data.game.addXp
import launcher.launcher.data.game.checkIfStreakFailed
import launcher.launcher.data.game.continueStreak
import launcher.launcher.data.game.getStreakInfo
import launcher.launcher.data.game.getUserInfo
import launcher.launcher.data.game.saveStreakInfo
import launcher.launcher.data.game.saveUserInfo
import launcher.launcher.data.game.xpFromStreak
import launcher.launcher.ui.screens.game.StreakFailedDialog
import launcher.launcher.ui.screens.game.StreakUpDialog
import launcher.launcher.utils.VibrationHelper
import launcher.launcher.utils.formatHour
import org.checkerframework.checker.units.qual.s
import kotlin.collections.forEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

    val questHelper = QuestHelper(context)
    val questList =  questHelper.filterQuestForToday(questHelper.getQuestList())
    val coinHelper = CoinHelper(context)

    val currentDate = getCurrentDate()
    val completedQuests = remember { SnapshotStateList<String>() }
    val progress = (completedQuests.size.toFloat() / questList.size.toFloat()).coerceIn(0f,1f)

    var streakInfo = remember {mutableStateOf(getStreakInfo(context))}

    val streakCompletedDialogVisible = remember { mutableStateOf(false) }
    val streakDaysLost = remember { mutableIntStateOf(0) }
    val streakFreezersUsed = remember { mutableIntStateOf(0) }

    BackHandler {  }

    fun streakFailResultHandler(streakCheckReturn: StreakCheckReturn?){
        if(streakCheckReturn!=null){
            if(streakCheckReturn.streakFreezersUsed!=null){
                streakFreezersUsed.intValue = streakCheckReturn.streakFreezersUsed
                streakCompletedDialogVisible.value = true
            }
            if(streakCheckReturn.streakDaysLost!=null){
                streakDaysLost.intValue = streakCheckReturn.streakDaysLost
            }
        }
    }
    LaunchedEffect(completedQuests,streakInfo) {

        questList.forEach { item ->
            if (questHelper.isQuestCompleted(item.title, currentDate) == true) {
                completedQuests.add(item.title)
            }
            if (questHelper.isQuestRunning(item.title)) {
                viewQuest(item, navController)
            }
        }
        Log.d("streak data", streakInfo.toString())
        if (streakInfo.value.currentStreak != 0) {
            streakFailResultHandler(User.checkIfStreakFailed())
        }
        val data = context.getSharedPreferences("onboard", MODE_PRIVATE)

        if (completedQuests.size == questList.size && data.getBoolean("onboard",false)) {
            if (User.continueStreak()) {
                streakFreezersUsed.intValue = 0
                streakCompletedDialogVisible.value = true
            }
        }
    }

    if(streakCompletedDialogVisible.value){
        StreakUpDialog(streakFreezersUsed.intValue) {
            streakCompletedDialogVisible.value = false
        }
    }
    if(streakDaysLost.intValue!=0){
        StreakFailedDialog(streakDaysLost.intValue) {
            streakDaysLost.intValue = 0
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        // Wait for the first touch down event
                        awaitFirstDown()
                        var dragAmount = 0f

                        // Track vertical drag events
                        do {
                            val event = awaitPointerEvent()
                            val dragEvent = event.changes.first()
                            val dragChange = dragEvent.positionChange().y
                            dragAmount += dragChange

                            // If the swipe exceeds the threshold, trigger navigation
                            if (dragAmount < -5) { // Swipe-up threshold
                                navController.navigate(Screen.AppList.route)
                                VibrationHelper.vibrate(50)
                                break
                            }
                        } while (dragEvent.pressed)
                    }
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
            Text(
                text = "${coinHelper.getCoinCount()} coins | Streak: ${streakInfo.value.currentStreak}D",
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.weight(1f)) // Pushes the Icon to the right

            Icon(
                painter = painterResource(id = R.drawable.outline_progress_activity_24),
                contentDescription = "user info and stats",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
                    .clickable {
                        navController.navigate(Screen.QuestStats.route)
                    }
            )
            // Profile icon on the right
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "user info and stats",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
                    .clickable {
                        navController.navigate(Screen.UserInfo.route)
                    }
            )
        }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {

            LiveClock(
                Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

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

            LazyColumn {
                items(questList.size){ index ->
                    val baseQuest = questList[index]
                    val timeRange = "${formatHour(baseQuest.timeRange[0])} - ${formatHour(baseQuest.timeRange[1])} : "
                    val prefix = if(baseQuest.timeRange[0]==0&&baseQuest.timeRange[1]==24) "" else timeRange
                    QuestItem(
                        text =  if(QuestHelper.isInTimeRange(baseQuest) && QuestHelper.isOver(baseQuest)) baseQuest.title else  prefix +  baseQuest.title,
                        isCompleted = completedQuests.contains(baseQuest.title),
                        isFailed = QuestHelper.isOver(baseQuest),
                        modifier = Modifier.clickable {
                            viewQuest(baseQuest,navController)
                    })
                }
                item {
                    QuestItem(
                        text = "List quests",
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.ListAllQuest.route)
                        }
                    )
                }
            }

        }
    }}
}

fun viewQuest(baseQuest: BasicQuestInfo, navController: NavController) {
    val data = Json.encodeToString<BasicQuestInfo>(baseQuest )
    navController.navigate(Screen.ViewQuest.route + data)
}
@Composable
fun QuestItem(
    text: String,
    isCompleted: Boolean = false,
    isFailed: Boolean = false,
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
        color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
}
