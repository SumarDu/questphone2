package launcher.launcher.ui.screens.quest

import androidx.compose.runtime.Composable
import launcher.launcher.config.Integration
import launcher.launcher.data.quest.BasicQuestInfo

@Composable
fun ViewQuest(
    basicQuestInfo: BasicQuestInfo
) {
    Integration.viewScreens[basicQuestInfo.integrationId.name]?.invoke(basicQuestInfo)
}