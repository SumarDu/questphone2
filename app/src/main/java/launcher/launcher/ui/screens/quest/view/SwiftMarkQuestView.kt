package launcher.launcher.ui.screens.quest.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import launcher.launcher.data.game.getUserInfo
import launcher.launcher.data.game.xpToRewardForQuest
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.data.quest.QuestDatabaseProvider
import launcher.launcher.ui.screens.quest.checkForRewards
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.formatHour
import launcher.launcher.utils.getCurrentDate

@Composable
fun SwiftMarkQuestView(
    basicQuestInfo: BasicQuestInfo
) {
    val context = LocalContext.current
    val questHelper = QuestHelper(context)
    val isQuestComplete = remember {
        mutableStateOf(
            basicQuestInfo.lastCompletedOn == getCurrentDate()
        )
    }
    val scope = rememberCoroutineScope()
    val dao = QuestDatabaseProvider.getInstance(context).questDao()

    val isInTimeRange = remember { mutableStateOf(QuestHelper.isInTimeRange(basicQuestInfo)) }
    val isFailed = remember { mutableStateOf(questHelper.isOver(basicQuestInfo)) }

    val progress = remember {
        mutableFloatStateOf(if (isQuestComplete.value || isFailed.value ) 1f else 0f)
    }


    val userInfo = getUserInfo(LocalContext.current)

    fun onQuestCompleted(){
        progress.floatValue = 1f
        scope.launch {
            basicQuestInfo.lastCompletedOn = getCurrentDate()
            dao.upsertQuest(basicQuestInfo)
        }
        checkForRewards(basicQuestInfo)
        isQuestComplete.value = true
    }

    BaseQuestView(
        hideStartQuestBtn = isQuestComplete.value || !isInTimeRange.value || isFailed.value,
        onQuestStarted = {
            onQuestCompleted()
        },
        progress = progress,
        loadingAnimationDuration = 400,
        startButtonTitle = "Mark as complete",
        isFailed = isFailed,
        onQuestCompleted = {
            onQuestCompleted()
        },
        isQuestCompleted = isQuestComplete
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
                text = (if(isQuestComplete.value) "Next Reward" else "Reward") + ": ${basicQuestInfo.reward} coins + ${xpToRewardForQuest(userInfo.level)} xp",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
            )


            if(!isInTimeRange.value){
                Text(
                    text = "Time: ${formatHour(basicQuestInfo.timeRange[0])} to ${formatHour(basicQuestInfo.timeRange[1])}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )
            }

            MarkdownText(
                markdown = basicQuestInfo.instructions,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 32.dp, bottom = 4.dp)
            )
        }
    }
}