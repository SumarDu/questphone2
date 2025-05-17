package launcher.launcher.ui.screens.quest.view.ai_snap

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import launcher.launcher.data.quest.ai.snap.AiSnap
import launcher.launcher.ui.screens.quest.checkForRewards
import launcher.launcher.ui.screens.quest.view.BaseQuestView
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.formatHour
import launcher.launcher.utils.getCurrentDate

@Composable
fun AiSnapQuestView(
    basicQuestInfo: BasicQuestInfo
) {
    val context = LocalContext.current
    val questHelper = QuestHelper(context)
    val aiQuest = questHelper.getQuestInfo<AiSnap>(basicQuestInfo)
    val isQuestComplete = remember {
        mutableStateOf(
            basicQuestInfo.lastCompletedOn == getCurrentDate()
        )
    }
    var isCameraScreen = remember { mutableStateOf(false) }
    var isAiEvaluating = remember { mutableStateOf(false) }
    val userInfo = getUserInfo(LocalContext.current)


    val dao = QuestDatabaseProvider.getInstance(context).questDao()
    val scope = rememberCoroutineScope()

    val isInTimeRange = remember { mutableStateOf(QuestHelper.isInTimeRange(basicQuestInfo)) }
    val isFailed = remember { mutableStateOf(questHelper.isOver(basicQuestInfo)) }
    var progress = remember {
        mutableFloatStateOf(if (isQuestComplete.value || isFailed.value ) 1f else 0f)
    }
    BackHandler(isCameraScreen.value || isAiEvaluating.value) {
        isCameraScreen.value = false
        isAiEvaluating.value = false
    }

    fun onQuestComplete(){
        progress.floatValue = 1f
        basicQuestInfo.lastCompletedOn = getCurrentDate()
        scope.launch {
            dao.upsertQuest(basicQuestInfo)
        }
        isCameraScreen.value = false
        checkForRewards(basicQuestInfo)
        isQuestComplete.value = true
    }

    if(isAiEvaluating.value) {
        Log.d("aiQuest",aiQuest.toString())
        AiEvaluationScreen(isAiEvaluating,aiQuest?.taskDescription ?: "return error") { isComplete ->
            if(isComplete) onQuestComplete()
        }
    } else if (isCameraScreen.value) {
        CameraScreen(isAiEvaluating)
    }
    else {
        BaseQuestView(
            hideStartQuestBtn = isQuestComplete.value || isFailed.value ||  !isInTimeRange.value,
            onQuestStarted = {
                isCameraScreen.value = true
            },
            progress = progress,
            loadingAnimationDuration = 400,
            startButtonTitle = "Click Image",
            isFailed = isFailed,
            onQuestCompleted = {onQuestComplete()},
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
                    text = (if(isQuestComplete.value) "Reward" else "Next Reward") + ": ${basicQuestInfo.reward} coins + ${xpToRewardForQuest(userInfo.level)} xp",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )

                if(!isInTimeRange.value){
                    Text(
                        text = "Time: ${formatHour(basicQuestInfo.timeRange[0])} to ${formatHour(basicQuestInfo.timeRange[1])}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                    )
                }
                MarkdownText(
                    markdown = questHelper.getInstruction(basicQuestInfo.title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 32.dp, bottom = 4.dp)
                )
            }
        }
    }
}