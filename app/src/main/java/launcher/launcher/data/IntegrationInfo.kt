package launcher.launcher.data

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import launcher.launcher.R
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.ui.screens.quest.setup.deep_focus.SetDeepFocus
import launcher.launcher.ui.screens.quest.view.DeepFocusQuestView

/**
 *
 * @property icon
 * @property label
 * @property description
 * @property id
 * @property setupScreen
 * @property viewScreen The screen that shows up when user presses an item from the quest list on home screen. basically the screen where you do the quest
 */
data class IntegrationInfo(
    val icon: Int = R.drawable.baseline_extension_24,
    val label: String = "",
    val description: String = "",
    val id: IntegrationId = IntegrationId.DEEP_FOCUS,
    val setupScreen: @Composable (NavHostController) -> Unit = { navController -> SetDeepFocus(navController) },
    val viewScreen: @Composable (BasicQuestInfo) -> Unit = { baseQuest -> DeepFocusQuestView(baseQuest) },
    val isLoginRequired: Boolean = false
    )