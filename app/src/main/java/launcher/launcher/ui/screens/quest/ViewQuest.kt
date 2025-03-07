package launcher.launcher.ui.screens.quest

import androidx.compose.runtime.Composable
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.BaseQuest
import launcher.launcher.ui.screens.quest.view.DeepFocusQuestView

@Composable
fun ViewQuest(
    baseQuest: BaseQuest
) {
    when(baseQuest.integrationId){
        IntegrationId.APP_FOCUS -> TODO()
        IntegrationId.DEEP_FOCUS -> DeepFocusQuestView(baseQuest)
        IntegrationId.HEALTH -> TODO()
    }

}