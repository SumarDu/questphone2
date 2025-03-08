package launcher.launcher.ui.screens.quest

import androidx.compose.runtime.Composable
import launcher.launcher.config.Integration
import launcher.launcher.data.quest.BaseQuest

@Composable
fun ViewQuest(
    baseQuest: BaseQuest
) {
    Integration.viewScreens[baseQuest.integrationId.name]?.invoke(baseQuest)
}