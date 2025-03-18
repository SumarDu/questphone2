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
import launcher.launcher.config.Integration
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.navigation.SetupQuestScreen
import launcher.launcher.ui.screens.launcher.AppList
import launcher.launcher.ui.screens.launcher.HomeScreen
import launcher.launcher.ui.screens.onboard.OnBoardScreen
import launcher.launcher.ui.screens.quest.ListAllQuests
import launcher.launcher.ui.screens.quest.ViewQuest
import launcher.launcher.ui.screens.quest.setup.SetIntegration
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
                        startDestination = Screen.OnBoard.route) {


                        composable(Screen.OnBoard.route) {
                            OnBoardScreen(navController)
                        }
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
                            val basicQuestInfo = json?.let { Json.decodeFromString<BasicQuestInfo>(it) }

                            ViewQuest(basicQuestInfo!!)
                        }

                        navigation(startDestination = SetupQuestScreen.Integration.route, route = Screen.AddNewQuest.route){
                            composable(SetupQuestScreen.Integration.route) {
                                SetIntegration(
                                    navController
                                )
                            }
                            Integration.setupRoutes.forEach{ integrationInfo ->
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
