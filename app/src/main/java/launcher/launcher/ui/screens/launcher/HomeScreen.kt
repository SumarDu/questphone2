package launcher.launcher.ui.screens.launcher

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import launcher.launcher.models.quest.BaseQuestInfo
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.launcher.components.LiveClock
import launcher.launcher.ui.screens.launcher.components.ProgressBar
import launcher.launcher.ui.screens.launcher.components.QuestItem
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.QuestListHelper
import launcher.launcher.utils.getCurrentDate


@Composable
fun HomeScreen(navController: NavController) {
    val questListHelper = QuestListHelper(LocalContext.current)
    val questList =  questListHelper.filterQuestsForToday(questListHelper.getQuestList())
    val coinHelper = CoinHelper(LocalContext.current)

    val currentDate = getCurrentDate()

    val completedQuests: MutableSet<String> = mutableSetOf()
    questList.forEach{item ->
        if(questListHelper.isQuestCompleted(item.title,currentDate) == true){
            completedQuests.add(item.title)
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
                modifier = Modifier.padding(24.dp)
                    .align(Alignment.End)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.Center)
        ) {

            LiveClock(
                Modifier.padding(bottom = 16.dp)
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



            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(50.dp)
                    .padding(bottom = 32.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                ProgressBar(
                    modifier = Modifier.fillMaxSize(),
                    progress = completedQuests.size / questList.size
                )
            }

            LazyColumn {
                items(questList.size){ index ->
                    val baseQuest = questList[index]
                    QuestItem(
                        text =  baseQuest.title,
                        isCompleted = completedQuests.contains(baseQuest.title),
                        modifier = Modifier.clickable {
                            val data = Json.encodeToString<BaseQuestInfo>(baseQuest )
                            navController.navigate(Screen.ViewQuest.route + data)
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

            // Core Apps button
            Text(
                text = "Core Apps >",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        navController.navigate(Screen.AppList.route)
                    }
            )
        }
    }}
}