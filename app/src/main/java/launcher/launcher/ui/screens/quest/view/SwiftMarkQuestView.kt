package launcher.launcher.ui.screens.quest.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.getCurrentDate

@Composable
fun SwiftMarkQuestView(
    basicQuestInfo: BasicQuestInfo
) {
    val context = LocalContext.current
    val questHelper = QuestHelper(context)
    val isQuestComplete = remember {
        mutableStateOf(
            questHelper.isQuestCompleted(
                basicQuestInfo.title,
                getCurrentDate()
            ) ?: false
        )
    }

    val progress = remember {
        mutableFloatStateOf(if (isQuestComplete.value) 1f else 0f)
    }
    BaseQuestView(
        hideStartQuestBtn = isQuestComplete.value,
        onQuestStarted = {
            isQuestComplete.value = true
            progress.floatValue = 1f
            questHelper.markQuestAsComplete(basicQuestInfo.title, getCurrentDate())
        },
        progress = progress,
        loadingAnimationDuration = 400
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = basicQuestInfo.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                fontFamily = JetBrainsMonoFont,
                modifier = Modifier.padding(top = 40.dp)
            )

            Text(
                text = "Reward: ${basicQuestInfo.reward} coins",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
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