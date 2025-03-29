package launcher.launcher.ui.screens.quest.view.ai_snap

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import io.github.jan.supabase.auth.auth
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.data.quest.ai.snap.AiSnap
import launcher.launcher.ui.screens.account.LoginScreen
import launcher.launcher.ui.screens.quest.view.BaseQuestView
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.Supabase
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
            questHelper.isQuestCompleted(
                basicQuestInfo.title,
                getCurrentDate()
            ) == true
        )
    }
    var isCameraScreen = remember { mutableStateOf(false) }
    var isAiEvaluating = remember { mutableStateOf(false) }


    val progress = remember {
        mutableFloatStateOf(if (isQuestComplete.value) 1f else 0f)
    }

    BackHandler(isCameraScreen.value || isAiEvaluating.value) {
        isCameraScreen.value = false
        isAiEvaluating.value = false
    }
    if(isAiEvaluating.value) {
        Log.d("aiQuest",aiQuest.toString())
        AiEvaluationScreen(isAiEvaluating,aiQuest?.taskDescription ?: "return error") { isComplete ->
            if(isComplete) {
                isQuestComplete.value = true
                progress.floatValue = 1f
                questHelper.markQuestAsComplete(basicQuestInfo, getCurrentDate())
                isCameraScreen.value = false
            }
        }
    } else if (isCameraScreen.value) {
        CameraScreen(isAiEvaluating)
    }
    else {
        BaseQuestView(
            hideStartQuestBtn = isQuestComplete.value,
            onQuestStarted = {
                isCameraScreen.value = true
            },
            progress = progress,
            loadingAnimationDuration = 400,
            startButtonTitle = "Click Image"
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

                MarkdownText(
                    markdown = questHelper.getInstruction(basicQuestInfo.title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 32.dp, bottom = 4.dp)
                )
            }
        }
    }
}