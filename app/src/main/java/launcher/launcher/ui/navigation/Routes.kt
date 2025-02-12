package launcher.launcher.ui.navigation

sealed class Screen(val route: String) {
    data object QuestTracker : Screen("quest_tracker")
    data object AppList : Screen("app_list")

    data object ViewQuest : Screen("view_quest")

    data object SetQuestInfo : Screen("set_quest_info")
    data object SetIntegration : Screen("set_quest_integration")

    data object SetFocusIntegration : Screen("set_focus_integration")

    data object SetAppFocusIntegration : Screen("set_app_focus_integration")

}