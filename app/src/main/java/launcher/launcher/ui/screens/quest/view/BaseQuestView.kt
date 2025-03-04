package launcher.launcher.ui.screens.quest.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import launcher.launcher.Constants
import launcher.launcher.data.quest.DeepFocus
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.getCurrentDate

@Composable
fun BaseQuestView(isQuestComplete: Boolean, onQuestStarted: () -> Unit, questViewBody : @Composable () -> Unit) {
    val coinHelper = CoinHelper(LocalContext.current)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if(!isQuestComplete) {
                Button(
                    onClick = {
                        onQuestStarted()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(text = "Start Quest")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
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

            questViewBody()
        }
    }
}