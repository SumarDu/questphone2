package launcher.launcher.ui.screens.quest

import androidx.compose.runtime.Composable
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.IntegrationInformation
import launcher.launcher.data.quest.BaseQuest
import launcher.launcher.ui.screens.quest.view.DeepFocusQuestView

@Composable
fun ViewQuest(
    baseQuest: BaseQuest
) {
    IntegrationInformation.viewScreens[baseQuest.integrationId.name]?.invoke(baseQuest)
}