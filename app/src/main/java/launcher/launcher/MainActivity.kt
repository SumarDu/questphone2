package launcher.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.serialization.json.Json
import launcher.launcher.data.IntegrationInfo
import launcher.launcher.data.IntegrationInformation
import launcher.launcher.data.quest.BaseQuest
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.navigation.SetupQuestScreen
import launcher.launcher.ui.screens.launcher.AppList
import launcher.launcher.ui.screens.launcher.HomeScreen
import launcher.launcher.ui.screens.quest.ListAllQuests
import launcher.launcher.ui.screens.quest.ViewQuest
import launcher.launcher.ui.screens.quest.setup.deep_focus.SetDeepFocus
import launcher.launcher.ui.screens.quest.setup.SetIntegration
import launcher.launcher.ui.screens.quest.setup.app_focus.SetAppFocus
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
                            HomeScreen(navController)
                        }
                        composable(Screen.AppList.route) {
                            AppList(
                                onNavigateToQuestTracker = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.ListAllQuest.route) {
                            ListAllQuests(navController)
                        }
                        composable(
                            route = "${Screen.ViewQuest.route}{baseQuestInfo}",
                            arguments = listOf(navArgument("baseQuestInfo") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val json = backStackEntry.arguments?.getString("baseQuestInfo")
                            val baseQuest = json?.let { Json.decodeFromString<BaseQuest>(it) }

                            ViewQuest(baseQuest!!)
                        }

                        navigation(startDestination = SetupQuestScreen.Integration.route, route = Screen.AddNewQuest.route){
                            composable(SetupQuestScreen.Integration.route) {
                                SetIntegration(
                                    navController
                                )
                            }
                            IntegrationInformation.setupRoutes.forEach{ integrationInfo ->
                                composable(route=integrationInfo.value.first) {
                                    integrationInfo.value.second.invoke()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
