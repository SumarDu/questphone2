package launcher.launcher.config

import launcher.launcher.R
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.IntegrationInfo
import launcher.launcher.ui.screens.quest.setup.deep_focus.SetDeepFocus
import launcher.launcher.ui.screens.quest.setup.health_connect.SetHealthConnect
import launcher.launcher.ui.screens.quest.view.DeepFocusQuestView


class Integration {
    companion object {
        val allInfo = listOf(
            IntegrationInfo(
                icon = R.drawable.baseline_timer_24,
                label = "Deep Focus",
                description = "Block all apps except the essential ones for a set period, allowing you to stay focused on your work.",
                id = IntegrationId.DEEP_FOCUS,
                setupScreen = {SetDeepFocus()},
                viewScreen = { baseQuest ->  DeepFocusQuestView(baseQuest) }
            ),
            IntegrationInfo(
                icon = R.drawable.baseline_directions_run_24,
                label = "Health Connect",
                description = "Earn coins for performing health related stuff like steps, water intake and more",
                setupScreen = { SetHealthConnect() },
                id = IntegrationId.HEALTH_CONNECT
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