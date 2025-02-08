package launcher.launcher.ui.screens.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import launcher.launcher.ui.theme.JetBrainsMonoFont

@Composable
fun ViewQuest(
    onNavigateToQuestTracker: () -> Unit,
    coins: Int = 100,
    reward: Int = 10,
    instructions: List<String> = listOf("Revise 0.C", "Practice Maths", "Complete Home-Works")
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Button(
                onClick = { /* Start quest */ },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(text = "Start Quest")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->

        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(innerPadding),
            horizontalAlignment = Alignment.Start,

            ) {
            // Coins display
            Text(
                text = "100 coins",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp)
                    .align(Alignment.End)
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Study 3hrs",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    fontFamily = JetBrainsMonoFont,
                    modifier = Modifier.padding(top = 40.dp,)
                )

                Text(
                    text = "Reward: 10 coins",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 32.dp, bottom = 4.dp)
                )

                LazyColumn {
                    items(instructions) {
                        Text(
                            text = "\u2022 $it",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}