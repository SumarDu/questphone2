package launcher.launcher.ui.screens.quest.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.data.quest.focus.AppFocus
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.getCurrentDate

@Composable
fun AppFocusQuestView(
    basicQuestInfo: BasicQuestInfo
) {
    val questHelper = QuestHelper(LocalContext.current)
    val appFocus = questHelper.getQuestInfo<AppFocus>(basicQuestInfo) ?: return
    val duration = appFocus.nextFocusDurationInMillis
    val isQuestComplete = questHelper.isQuestCompleted(basicQuestInfo.title, getCurrentDate()) ?: false

    BaseQuestView(
        hideStartQuestBtn = isQuestComplete,
        onQuestStarted = {

        }) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = basicQuestInfo.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                fontFamily = JetBrainsMonoFont,
                modifier = Modifier.padding(top = 40.dp,)
            )

            Text(
                text = "Reward: ${basicQuestInfo.reward} coins",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
            )

            Text(
                text = if (!isQuestComplete) "Duration: ${duration / 60_000}m" else "Next Duration: ${duration / 60_000}m",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                modifier = Modifier.padding(top = 32.dp)
            )

            val pm = LocalContext.current.packageManager
            val appName = pm.getApplicationLabel(pm.getApplicationInfo(appFocus.selectedFocusApp, 0)).toString()

            Text(
                text = "Focus App: $appName",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                modifier = Modifier.padding(bottom = 8.dp)
            )


            Text(
                text = "Instructions",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 32.dp, bottom = 4.dp)
            )
            Text(
                text = basicQuestInfo.instructions,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(4.dp)
            )
        }
    }

}

