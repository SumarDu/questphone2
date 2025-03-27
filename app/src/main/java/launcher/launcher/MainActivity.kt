package launcher.launcher

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import launcher.launcher.config.Integration
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.navigation.SetupQuestScreen
import launcher.launcher.ui.screens.account.ForgotPasswordScreen
import launcher.launcher.ui.screens.game.StoreScreen
import launcher.launcher.ui.screens.launcher.AppList
import launcher.launcher.ui.screens.launcher.HomeScreen
import launcher.launcher.ui.screens.account.LoginScreen
import launcher.launcher.ui.screens.account.SignUpScreen
import launcher.launcher.ui.screens.onboard.OnBoardScreen
import launcher.launcher.ui.screens.quest.ListAllQuests
import launcher.launcher.ui.screens.quest.ViewQuest
import launcher.launcher.ui.screens.quest.setup.SetIntegration
import launcher.launcher.ui.theme.LauncherTheme
import launcher.launcher.utils.Supabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val data = getSharedPreferences("onboard", MODE_PRIVATE)
        Supabase.supabase.handleDeeplinks(intent)

        setContent {
            LauncherTheme {
                var isUserOnboarded = remember {mutableStateOf(true)}
                Surface {
                    LaunchedEffect(isUserOnboarded) {
                        isUserOnboarded.value = data.getBoolean("onboard",false)
                        Log.d("onboard", isUserOnboarded.value.toString())
                    }
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Screen.HomeScreen.route
//                            if (!isUserOnboarded.value) Screen.OnBoard.route
//                            else
                    ) {

                        composable(Screen.Login.route) {
                            LoginScreen(
                                navController,

                            )

                        }
                        composable(Screen.SignUp.route) {
                            SignUpScreen(
                                navController
                            )

                        }
                        composable(Screen.ForgetPassword.route) {
                            ForgotPasswordScreen(
                                navController
                            )

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
                            Log.d("viewQuest",
                                backStackEntry.arguments?.getString("baseQuestInfo").toString()
                            )
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
