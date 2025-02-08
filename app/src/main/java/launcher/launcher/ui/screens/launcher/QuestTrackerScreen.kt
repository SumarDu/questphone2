package launcher.launcher.ui.screens.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import launcher.launcher.R
import launcher.launcher.ui.screens.launcher.components.QuestItem


@Composable
fun QuestTrackerScreen(onNavigateToAppList: () -> Unit, onNavigateToViewQuest: () -> Unit, onNavigateToEditQuest: () -> Unit) {
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
                            if (dragAmount < -100) { // Swipe-up threshold
                                onNavigateToAppList()
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
            // Simple house and trees ASCII art representation
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.img),
                modifier = Modifier.padding(bottom = 24.dp)
                    .align(Alignment.CenterHorizontally),
                contentDescription = "ascii art"
            )

            // QUESTS header
            Text(
                text = "QUESTS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(50.dp)
                    .padding(bottom = 32.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Background
                    drawRect(
                        color = Color.DarkGray,
                        size = size
                    )
                    // Progress (20%)
                    drawRect(
                        color = Color.White,
                        size = size.copy(width = size.width * 0.2f)
                    )
                }
            }

            // Quest items
            QuestItem("Study 3h",modifier = Modifier.clickable {
                onNavigateToViewQuest()
            })
            QuestItem("Walk 3 kms", modifier = Modifier.clickable {
                onNavigateToViewQuest()
            })
            QuestItem(
                text = "Read a book",
                isCompleted = true,
                modifier = Modifier.clickable {
                    onNavigateToViewQuest()
                }
            )
            QuestItem(
                text = "Add a quest",
                isCompleted = false,
                modifier = Modifier.clickable {
                    onNavigateToEditQuest()
                }
            )

            // Core Apps button
            Text(
                text = "Core Apps >",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        onNavigateToAppList()
                    }
            )
        }
    }}
}