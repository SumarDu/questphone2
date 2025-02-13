package launcher.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.launcher.AppList
import launcher.launcher.ui.screens.launcher.QuestTrackerScreen
import launcher.launcher.ui.screens.quest.setup.EditQuest
import launcher.launcher.ui.screens.quest.SetIntegration
import launcher.launcher.ui.screens.quest.ViewQuest
import launcher.launcher.ui.screens.quest.setup.SetAppFocusIntegration
import launcher.launcher.ui.screens.quest.setup.SetFocusIntegration
import launcher.launcher.ui.theme.LauncherTheme



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LauncherTheme {
                Surface {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Screen.QuestTracker.route) {
                        composable(Screen.QuestTracker.route) {
                            QuestTrackerScreen(
                                onNavigateToAppList = {
                                    navController.navigate(Screen.AppList.route)
                                },
                                onNavigateToViewQuest = {
                                    navController.navigate(Screen.ViewQuest.route)
                                },
                                onNavigateToEditQuest = {
                                    navController.navigate(Screen.SetIntegration.route)
                                }
                            )
                        }
                        composable(Screen.AppList.route) {
                            AppList(
                                onNavigateToQuestTracker = {
                                    navController.popBackStack() // Navigate back to QuestTracker
                                }
                            )
                        }

                        composable(Screen.SetQuestInfo.route) {
                            EditQuest(navController)
                        }

                        composable(Screen.ViewQuest.route) {
                            ViewQuest(
                                onNavigateToQuestTracker = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.SetIntegration.route) {
                            SetIntegration(
                                navController
                            )
                        }
                        composable(Screen.SetFocusIntegration.route) {
                            SetFocusIntegration(navController)
                        }
                        composable(Screen.SetAppFocusIntegration.route) {
                            SetAppFocusIntegration(navController)
                        }
                    }
                }
            }
        }
    }
}
