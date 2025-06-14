package launcher.launcher.data

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable
import launcher.launcher.R
import launcher.launcher.data.quest.CommonQuestInfo
import launcher.launcher.ui.screens.quest.setup.deep_focus.SetDeepFocus
import launcher.launcher.ui.screens.quest.view.DeepFocusQuestView

@Serializable
enum class IntegrationId(
    val icon: Int = R.drawable.baseline_extension_24,
    val label: String = "",
    val description: String = "",
    val setupScreen: @Composable (String?, NavHostController) -> Unit = { id, navController -> SetDeepFocus(id,navController) },
    val viewScreen: @Composable (CommonQuestInfo) -> Unit = { baseQuest -> DeepFocusQuestView(baseQuest) },
    val isLoginRequired: Boolean = false,
    val rewardCoins: Int = 5,
    val docLink : String = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/AiSnap.md"
) {
    /**
     * blocks all apps except a few selected ones.
     * Used in scenarios wherein user wants to block everything except a few necessary apps like phone, messaging, gmail, music etc.
     * Useful when user wants to block access to his phone and focus on some irl task like studying
     */
    DEEP_FOCUS(
        icon = R.drawable.deep_focus_icon,
        label = "Deep Focus",
        description = "Block all apps except the essential ones for a set period, allowing you to stay focused on your work.",
        setupScreen = {id,navController -> SetDeepFocus(id,navController) },
        viewScreen = { baseQuest ->  DeepFocusQuestView(baseQuest) },
        docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/DeepFocus.md"
    ),


    HEALTH_CONNECT(
        icon = R.drawable.health_icon,
        label = "Health Connect",
        description = "Earn coins for performing health related stuff like steps, water intake and more",
        setupScreen = { id,navController ->
            launcher.launcher.ui.screens.quest.setup.health_connect.SetHealthConnect(
                id,navController
            )
        },
        viewScreen = { baseQuest ->
            launcher.launcher.ui.screens.quest.view.HealthQuestView(
                baseQuest
            )
        },
        docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/HealthConnect.md"
    ),

    SWIFT_MARK(
        icon = R.drawable.swift_mark_icon,
        label = "Swift Mark",
        description = "Just mark it as done and earn coins instantly. No verification needed—your honesty is the key!",
        setupScreen = {id,navController ->
            launcher.launcher.ui.screens.quest.setup.swift_mark.SetSwiftMark(id,
                navController
            )
        },
        viewScreen = { baseQuest ->
            launcher.launcher.ui.screens.quest.view.SwiftMarkQuestView(
                baseQuest
            )
        },
        rewardCoins = 1,
        docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/SwiftMark.md"
    ),

    AI_SNAP(
        icon = R.drawable.ai_snap_icon,
        label = "AI verified Snap",
        description = "Complete the task, snap a pic, and let AI verify your progress!",
        setupScreen = {id, navController ->
            launcher.launcher.ui.screens.quest.setup.ai_snap.SetAiSnap(
                id,
                navController
            )
        },
        viewScreen = { baseQuest ->
            launcher.launcher.ui.screens.quest.view.ai_snap.AiSnapQuestView(
                baseQuest
            )
        },
        isLoginRequired = true,
        docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/AiSnap.md"
    )
}