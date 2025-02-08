package launcher.launcher.ui.navigation

sealed class Screen(val route: String) {
    object QuestTracker : Screen("quest_tracker")
    object AppList : Screen("app_list")

    object ViewQuest : Screen("view_quest")

    object EditQuest : Screen("edit_quest")
}