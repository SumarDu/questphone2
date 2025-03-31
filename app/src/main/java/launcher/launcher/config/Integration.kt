package launcher.launcher.config

import launcher.launcher.R
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.IntegrationInfo
import launcher.launcher.ui.screens.quest.setup.ai_snap.SetAiSnap
import launcher.launcher.ui.screens.quest.setup.deep_focus.SetDeepFocus
import launcher.launcher.ui.screens.quest.setup.health_connect.SetHealthConnect
import launcher.launcher.ui.screens.quest.setup.swift_mark.SetSwiftMark
import launcher.launcher.ui.screens.quest.view.DeepFocusQuestView
import launcher.launcher.ui.screens.quest.view.HealthQuestView
import launcher.launcher.ui.screens.quest.view.SwiftMarkQuestView
import launcher.launcher.ui.screens.quest.view.ai_snap.AiSnapQuestView


class Integration {
    companion object {
        val allInfo = listOf(
            IntegrationInfo(
                icon = R.drawable.baseline_timer_24,
                label = "Deep Focus",
                description = "Block all apps except the essential ones for a set period, allowing you to stay focused on your work.",
                id = IntegrationId.DEEP_FOCUS,
                setupScreen = {navController -> SetDeepFocus(navController) },
                viewScreen = { baseQuest ->  DeepFocusQuestView(baseQuest) },
                docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/DeepFocus.md"
            ),
            IntegrationInfo(
                icon = R.drawable.baseline_directions_run_24,
                label = "Health Connect",
                description = "Earn coins for performing health related stuff like steps, water intake and more",
                setupScreen = { navController ->  SetHealthConnect(navController) },
                viewScreen = { baseQuest -> HealthQuestView(baseQuest)},
                id = IntegrationId.HEALTH_CONNECT,
                docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/HealthConnect.md"
            ),
            IntegrationInfo(
                icon = R.drawable.baseline_bolt_24,
                label = "Swift Mark",
                description = "Just mark it as done and earn coins instantly. No verification needed—your honesty is the key!",
                setupScreen = {navController -> SetSwiftMark(navController) },
                viewScreen = { baseQuest -> SwiftMarkQuestView(baseQuest) },
                id = IntegrationId.SWIFT_MARK,
                docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/SwiftMark.md"
            ),
            IntegrationInfo(
                icon = R.drawable.baseline_camera_24,
                label = "AI verified Snap",
                description = "Complete the task, snap a pic, and let AI verify your progress!",
                setupScreen = { navController -> SetAiSnap(navController) },
                viewScreen = { baseQuest -> AiSnapQuestView(baseQuest) },
                id = IntegrationId.AI_SNAP,
                isLoginRequired = true,
                docLink = "https://raw.githubusercontent.com/nethical6/BlankPhoneQuestDocs/refs/heads/main/quest/AiSnap.md"
            )
        )


        /**
         * Returns all the auto generated routes of the setup screens for the various quests
         *
         * Format:
         *
         * integration_id.name -> (route_id,  setup_screen_fun)
         */
        val setupRoutes = allInfo
            .associate { it.id.name to Pair("setup_${it.id.name}", it.setupScreen)  }

        /**
         *  Returns all the screens that need to be opened when user tries to view a quest
         *
         *  Format:
         *
         *  integration_id.name -> view_quest_screen
         */
        val viewScreens = allInfo
            .associate {  it.id.name to it.viewScreen}

    }
}