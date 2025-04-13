package launcher.launcher

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.serialization.json.Json
import launcher.launcher.config.Integration
import launcher.launcher.data.game.UserInfo
import launcher.launcher.data.game.getUserInfo
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.navigation.SetupQuestScreen
import launcher.launcher.ui.screens.game.StoreScreen
import launcher.launcher.ui.screens.game.UserInfoScreen
import launcher.launcher.ui.screens.launcher.AppList
import launcher.launcher.ui.screens.launcher.HomeScreen
import launcher.launcher.ui.screens.onboard.OnBoardScreen
import launcher.launcher.ui.screens.quest.ListAllQuests
import launcher.launcher.ui.screens.quest.ViewQuest
import launcher.launcher.ui.screens.quest.setup.SetIntegration
import launcher.launcher.ui.screens.quest.stats.QuestStatsView
import launcher.launcher.ui.theme.LauncherTheme
import launcher.launcher.utils.Supabase
import launcher.launcher.utils.VibrationHelper


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val data = getSharedPreferences("onboard", MODE_PRIVATE)
        Supabase.supabase.handleDeeplinks(intent)

        setContent {
            var isUserOnboarded = remember {mutableStateOf(true)}
            LaunchedEffect(isUserOnboarded.value) {
                isUserOnboarded.value = data.getBoolean("onboard",false)
                Log.d("onboard", isUserOnboarded.value.toString())
            }
            LauncherTheme {
                Surface {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = if (!isUserOnboarded.value) Screen.OnBoard.route
                                else Screen.HomeScreen.route,
                    ) {

                        composable(Screen.UserInfo.route) {
                            UserInfoScreen()
                        }
                        composable(Screen.OnBoard.route) {
                            OnBoardScreen(navController)
                        }

                        composable(Screen.HomeScreen.route) {
                            HomeScreen(navController)
                        }

                        composable(Screen.Store.route) {
                            StoreScreen(navController)
                        }
                        composable(Screen.AppList.route) {
                            AppList()
                        }

                        composable(Screen.ListAllQuest.route) {
                            ListAllQuests(navController)
                        }
                        composable(
                            route = "${Screen.ViewQuest.route}{baseQuestInfo}",
                            arguments = listOf(navArgument("baseQuestInfo") { type = NavType.StringType })
                        ) { backStackEntry ->
                            Log.d("viewQuest",
                                backStackEntry.arguments?.getString("baseQuestInfo").toString()
                            )
                            val json = backStackEntry.arguments?.getString("baseQuestInfo")
                            val basicQuestInfo = json?.let { Json.decodeFromString<BasicQuestInfo>(it) }

                            ViewQuest(navController,basicQuestInfo!!)
                        }

                        navigation(startDestination = SetupQuestScreen.Integration.route, route = Screen.AddNewQuest.route){
                            composable(SetupQuestScreen.Integration.route) {
                                SetIntegration(
                                    navController
                                )
                            }
                            Integration.setupRoutes.forEach{ integrationInfo ->
                                composable(route=integrationInfo.value.first) {
                                    integrationInfo.value.second.invoke(navController)
                                }
                            }
                        }
                        composable(Screen.QuestStats.route) {
                            QuestStatsView()
                        }
                    }
                }
            }
        }
    }
}
