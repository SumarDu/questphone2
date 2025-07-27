package neth.iecal.questphone.ui.navigation

/**
 * Main screen navigation
 *
 * @property route
 */
sealed class Screen(val route: String) {
    data object HomeScreen : Screen("home_screen/")
    data object AppList : Screen("app_list/")
    data object ViewQuest : Screen("view_quest/")
    data object AddNewQuest : Screen("add_quest/")
    data object ListAllQuest : Screen("list_quest/")

    data object OnBoard : Screen("onboard/")
    data object ResetPass : Screen("reset_pass/")
    data object Store : Screen("store/")
    data object UserInfo : Screen("userInfo/")
    data object Settings : Screen("settings/")
    object GestureSettings : Screen("settings/gestures")
    object CalendarSyncSettings : Screen("settings/calendar_sync")
    data object QuestStats : Screen("quest_stats/")
    data object Stats : Screen("stats/")
    data object CreateAppUnlocker : Screen("create_app_unlocker/")

    data object SelectApps : Screen("select_apps/")

}

/**
 * All sub screens that show up when setting up a new quest
 *
 * @property route
 */
sealed class SetupQuestScreen(val route: String) {
    data object Integration : SetupQuestScreen("set_quest_integration/")
}

