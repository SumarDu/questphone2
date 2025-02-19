package launcher.launcher.ui.screens.quest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.Constants
import launcher.launcher.models.quest.BaseQuestInfo
import launcher.launcher.models.quest.FocusAppQuestInfo
import launcher.launcher.models.quest.FocusQuestInfo
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.CoinHelper
import launcher.launcher.utils.QuestListHelper

@Composable
fun ViewQuest(
    baseQuestInfo: BaseQuestInfo
) {
    val coinHelper = CoinHelper(LocalContext.current)

    var focusQuestInfo = FocusQuestInfo()
    var appFocusQuestInfo = FocusAppQuestInfo()


    var duration = 0L

    val questListHelper = QuestListHelper(LocalContext.current)
    when(baseQuestInfo.integrationId){
        Constants.INTEGRATION_ID_FOCUS -> {
            focusQuestInfo = questListHelper.getFocusQuestInfo(baseQuestInfo) ?: return
            duration = focusQuestInfo.nextFocusDuration
        }
        Constants.INTEGRATION_ID_APP_FOCUS -> {
            appFocusQuestInfo = questListHelper.getAppFocusQuestInfo(baseQuestInfo) ?: return
            duration = focusQuestInfo.nextFocusDuration

        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Button(
                onClick = {
                    coinHelper.incrementCoinCount(baseQuestInfo.reward)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(text = "Start Quest")
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

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = baseQuestInfo.title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    fontFamily = JetBrainsMonoFont,
                    modifier = Modifier.padding(top = 40.dp,)
                )

                Text(
                    text = "Reward: ${baseQuestInfo.reward} coins",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )

                Text(
                    text = "Duration: ${duration / 60_000}m",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                    modifier = Modifier.padding(top = 32.dp)
                )

                val pm = LocalContext.current.packageManager
                when(baseQuestInfo.integrationId){
                    Constants.INTEGRATION_ID_FOCUS -> {
                        var apps = ""
                        focusQuestInfo.unrestrictedApps.forEach { value ->
                            apps +=  pm.getApplicationLabel(pm.getApplicationInfo(value,0)).toString()
                            apps += " "
                        }
                        Text(
                            text = "Unrestricted Apps: $apps",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Constants.INTEGRATION_ID_APP_FOCUS -> {
                        val app = pm.getApplicationLabel(pm.getApplicationInfo(appFocusQuestInfo.selectedApp,0)).toString()
                        Text(
                            text = "Focus App: $app",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }



                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 32.dp, bottom = 4.dp)
                )

                LazyColumn {
                    items(baseQuestInfo.instructions) {
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