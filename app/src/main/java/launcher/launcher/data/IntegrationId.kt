package launcher.launcher.data

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import launcher.launcher.R
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.ui.screens.quest.setup.deep_focus.SetDeepFocus
import launcher.launcher.ui.screens.quest.view.DeepFocusQuestView

enum class IntegrationId(
    val icon: Int = R.drawable.baseline_extension_24,
    val label: String = "",
    val description: String = "",
    val setupScreen: @Composable (NavHostController) -> Unit = { navController -> SetDeepFocus(navController) },
    val viewScreen: @Composable (BasicQuestInfo) -> Unit = { baseQuest -> DeepFocusQuestView(baseQuest) },
    val isLoginRequired: Boolean = false,
    val docLink : String = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/AiSnap.md"
) {
    /**
     * blocks all apps except a few selected ones.
     * Used in scenarios wherein user wants to block everything except a few necessary apps like phone, messaging, gmail, music etc.
     * Useful when user wants to block access to his phone and focus on some irl task like studying
     */
    DEEP_FOCUS(
        icon = R.drawable.baseline_timer_24,
        label = "Deep Focus",
        description = "Block all apps except the essential ones for a set period, allowing you to stay focused on your work.",
        setupScreen = {navController -> SetDeepFocus(navController) },
        viewScreen = { baseQuest ->  DeepFocusQuestView(baseQuest) },
        docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/DeepFocus.md"
    ),


    HEALTH_CONNECT(
        icon = R.drawable.baseline_directions_run_24,
        label = "Health Connect",
        description = "Earn coins for performing health related stuff like steps, water intake and more",
        setupScreen = { navController ->
            launcher.launcher.ui.screens.quest.setup.health_connect.SetHealthConnect(
                navController
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
        icon = R.drawable.baseline_bolt_24,
        label = "Swift Mark",
        description = "Just mark it as done and earn coins instantly. No verification needed—your honesty is the key!",
        setupScreen = {navController ->
            launcher.launcher.ui.screens.quest.setup.swift_mark.SetSwiftMark(
                navController
            )
        },
        viewScreen = { baseQuest ->
            launcher.launcher.ui.screens.quest.view.SwiftMarkQuestView(
                baseQuest
            )
        },
        docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/SwiftMark.md"
    ),

    AI_SNAP(
        icon = R.drawable.baseline_camera_24,
        label = "AI verified Snap",
        description = "Complete the task, snap a pic, and let AI verify your progress!",
        setupScreen = { navController ->
            launcher.launcher.ui.screens.quest.setup.ai_snap.SetAiSnap(
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