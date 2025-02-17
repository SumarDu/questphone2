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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.launcher.components.LiveClock
import launcher.launcher.ui.screens.launcher.components.ProgressBar
import launcher.launcher.ui.screens.launcher.components.QuestItem


@Composable
fun HomeScreen(navController: NavController) {
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
                text = "100 coins",
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Quest items
            QuestItem("Study 3h",modifier = Modifier.clickable {
                navController.navigate(Screen.ViewQuest.route)
            })
            QuestItem("Walk 3 kms", modifier = Modifier.clickable {
                navController.navigate(Screen.ViewQuest.route)
            })
            QuestItem(
                text = "List quests",
                isCompleted = true,
                modifier = Modifier.clickable {
                    navController.navigate(Screen.ListAllQuest.route)
                }
            )
            QuestItem(
                text = "Add a quest",
                isCompleted = false,
                modifier = Modifier.clickable {
                    navController.navigate(Screen.AddNewQuest.route)
                }
            )

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