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
    data object DayQuests : Screen("day_quests/")

    data object OnBoard : Screen("onboard/")
    data object ResetPass : Screen("reset_pass/")
    data object Store : Screen("store/")
    data object UserInfo : Screen("userInfo/")
    data object Settings : Screen("settings/")
    object GestureSettings : Screen("settings/gestures")

    // New settings sub-screens
    data object SettingsCheckpoints : Screen("settings/checkpoints/")
    data object SettingsProtection : Screen("settings/protection/")
    data object SettingsOverduePenalties : Screen("settings/overdue_penalties/")
    data object SettingsUnplannedBreakReasons : Screen("settings/unplanned_break_reasons/")
    data object SettingsUnplannedQuestFilter : Screen("settings/unplanned_quest_filter/")
    data object SettingsCalendarSync : Screen("settings/calendar_sync/")
    data object SettingsAiTools : Screen("settings/ai_tools/")
    data object SettingsBackupsDev : Screen("settings/backups_dev/")

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

