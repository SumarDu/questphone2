package launcher.launcher.ui.screens.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import launcher.launcher.utils.VibrationHelper
import launcher.launcher.utils.formatHour

@Composable
fun HomeScreen(navController: NavController) {
    val questHelper = QuestHelper(LocalContext.current)
    val questList =  questHelper.filterQuestForToday(questHelper.getQuestList())
    val coinHelper = CoinHelper(LocalContext.current)

    val currentDate = getCurrentDate()
    val completedQuests = remember { SnapshotStateList<String>() }
    val progress = (completedQuests.size.toFloat() / questList.size.toFloat()).coerceIn(0f,1f)

    BackHandler {  }
    LaunchedEffect(completedQuests) {
        questList.forEach{item ->
            if(questHelper.isQuestCompleted(item.title, currentDate) == true){
                completedQuests.add(item.title)
            }
            if(questHelper.isQuestRunning(item.title)){
                viewQuest(item,navController)
            }
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,

        ) {
            // Coins display
            Text(
                text = "${coinHelper.getCoinCount()} coins",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.End)
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


