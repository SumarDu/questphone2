package launcher.launcher.ui.navigation

/**
 * Main screen navigation
 *
 * @property route
 */
sealed class Screen(val route: String) {
    data object HomeScreen : Screen("quest_tracker/")
    data object AppList : Screen("app_list/")
    data object ViewQuest : Screen("view_quest/")
    data object AddNewQuest : Screen("add_quest/")
    data object ListAllQuest : Screen("list_quest/")

    data object OnBoard : Screen("onboard/")
    data object Store : Screen("store/")
    data object UserInfo : Screen("userInfo/")
    data object QuestStats : Screen("questStats/")

}

/**
 * All sub screens that show up when setting up a new quest
 *
 * @property route
 */
sealed class SetupQuestScreen(val route: String) {
    data object Integration : SetupQuestScreen("set_quest_integration/")
}

