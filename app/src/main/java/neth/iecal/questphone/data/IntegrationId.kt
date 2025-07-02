package neth.iecal.questphone.data

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable
import neth.iecal.questphone.R
import neth.iecal.questphone.data.quest.CommonQuestInfo

import neth.iecal.questphone.ui.screens.quest.setup.deep_focus.SetDeepFocus
import neth.iecal.questphone.ui.screens.quest.setup.health_connect.SetHealthConnect
import neth.iecal.questphone.ui.screens.quest.setup.swift_mark.SetSwiftMark
import neth.iecal.questphone.ui.screens.quest.view.DeepFocusQuestView
import neth.iecal.questphone.ui.screens.quest.view.HealthQuestView
import neth.iecal.questphone.ui.screens.quest.view.SwiftMarkQuestView


@Serializable
enum class IntegrationId(
    val icon: Int = R.drawable.baseline_extension_24,
    val label: String = "",
    val description: String = "",
    val setupScreen: @Composable (String?, NavHostController) -> Unit = { id, navController ->
        SetDeepFocus(
            id,
            navController
        )
    },
    val viewScreen: @Composable (CommonQuestInfo) -> Unit = { baseQuest ->
        DeepFocusQuestView(
            baseQuest
        )
    },
    val isLoginRequired: Boolean = false,
    val rewardCoins: Int = 5,
    val docLink : String = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/AiSnap.md"
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
        setupScreen = {id,navController -> SetDeepFocus(id, navController) },
        viewScreen = { baseQuest -> DeepFocusQuestView(baseQuest) },
        docLink = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/DeepFocus.md"
    ),


    HEALTH_CONNECT(
        icon = R.drawable.health_icon,
        label = "Health Connect",
        description = "Earn coins for performing health related stuff like steps, water intake and more",
        setupScreen = { id,navController ->
            SetHealthConnect(
                id, navController
            )
        },
        viewScreen = { baseQuest ->
            HealthQuestView(
                baseQuest
            )
        },
        docLink = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/HealthConnect.md"
    ),

    SWIFT_MARK(
        icon = R.drawable.swift_mark_icon,
        label = "Swift Mark",
        description = "Just mark it as done and earn coins instantly. No verification needed—your honesty is the key!",
        setupScreen = {id,navController ->
            SetSwiftMark(
                id,
                navController
            )
        },
        viewScreen = { baseQuest ->
            SwiftMarkQuestView(
                baseQuest
            )
        },
        rewardCoins = 1,
        docLink = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/SwiftMark.md"
    ),


}