package launcher.launcher.ui.navigation

sealed class Screen(val route: String) {
    data object QuestTracker : Screen("quest_tracker")
    data object AppList : Screen("app_list")
    data object ViewQuest : Screen("view_quest/")
    data object AddNewQuest : Screen("add_quest")
    data object ListAllQuest : Screen("list_quest")
}

sealed class AddNewQuestSubScreens(val route: String) {
    data object QuestInfo : AddNewQuestSubScreens("set_quest_info")
    data object Integration : AddNewQuestSubScreens("set_quest_integration")
    data object FocusIntegration : AddNewQuestSubScreens("set_focus_integration")
    data object AppFocusIntegration : AddNewQuestSubScreens("set_app_focus_integration")
    data object ReviewQuest : AddNewQuestSubScreens("save_new_quest")
    data object SavingDialog : AddNewQuestSubScreens("saving_dialog")
}

