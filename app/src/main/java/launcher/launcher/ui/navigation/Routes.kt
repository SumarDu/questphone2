package launcher.launcher.ui.navigation

sealed class Screen(val route: String) {
    data object QuestTracker : Screen("quest_tracker")
    data object AppList : Screen("app_list")

    data object ViewQuest : Screen("view_quest")

    data object AddNewQuest : Screen("add_quest")
}

sealed class QuestSetupScreen(val route: String) {
    data object QuestInfo : QuestSetupScreen("set_quest_info")
    data object Integration : QuestSetupScreen("set_quest_integration")

    data object FocusIntegration : QuestSetupScreen("set_focus_integration")

    data object AppFocusIntegration : QuestSetupScreen("set_app_focus_integration")

}