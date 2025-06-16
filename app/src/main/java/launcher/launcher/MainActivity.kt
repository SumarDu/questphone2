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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.serialization.json.Json
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.CommonQuestInfo
import launcher.launcher.data.quest.QuestDatabaseProvider
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.navigation.SetupQuestScreen
import launcher.launcher.ui.screens.game.StoreScreen
import launcher.launcher.ui.screens.game.UserInfoScreen
import launcher.launcher.ui.screens.launcher.AppList
import launcher.launcher.ui.screens.launcher.HomeScreen
import launcher.launcher.ui.screens.onboard.OnBoardScreen
import launcher.launcher.ui.screens.pet.PetDialog
import launcher.launcher.ui.screens.quest.ListAllQuests
import launcher.launcher.ui.screens.quest.RewardDialogMaker
import launcher.launcher.ui.screens.quest.ViewQuest
import launcher.launcher.ui.screens.quest.setup.SetIntegration
import launcher.launcher.ui.screens.quest.stats.specific.BaseQuestStatsView
import launcher.launcher.ui.theme.LauncherTheme
import launcher.launcher.utils.Supabase
import launcher.launcher.utils.isOnline
import launcher.launcher.utils.triggerSync


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val data = getSharedPreferences("onboard", MODE_PRIVATE)
        Supabase.supabase.handleDeeplinks(intent)


        setContent {
            val isUserOnboarded = remember {mutableStateOf(true)}
            val isPetDialogVisible = remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                isUserOnboarded.value = data.getBoolean("onboard",false)
                Log.d("onboard", isUserOnboarded.value.toString())
            }
            LauncherTheme {
                Surface {
                    val navController = rememberNavController()
                    val currentRoute = navController.currentBackStackEntryAsState()

                    val dao = QuestDatabaseProvider.getInstance(applicationContext).questDao()

                    val unSyncedItems = remember { dao.getUnSyncedQuests() }
                    val context = LocalContext.current

                    RewardDialogMaker()
                    if(currentRoute != Screen.OnBoard){

                        PetDialog(
                            petId = "fluffy",
                            isPetDialogVisible,
                            navController
                        )
                    }
                    LaunchedEffect(Unit) {
                        unSyncedItems.collect {
                            if(context.isOnline()){
                                triggerSync(applicationContext)
                            }
                        }
                    }

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
                            val json = backStackEntry.arguments?.getString("baseQuestInfo")
                            val commonQuestInfo = json?.let { Json.decodeFromString<CommonQuestInfo>(it) }

                            ViewQuest(navController,commonQuestInfo!!)
                        }

                        navigation(startDestination = SetupQuestScreen.Integration.route, route = Screen.AddNewQuest.route){
                            composable(SetupQuestScreen.Integration.route) {
                                SetIntegration(
                                    navController
                                )
                            }
                            IntegrationId.entries.forEach{item ->
                                composable(route=item.name + "/{id}",
                                    arguments = listOf(navArgument("id") { type = NavType.StringType })) { backstack->
                                    var id = backstack.arguments?.getString("id")
                                    if(id=="ntg"){
                                        id = null
                                    }
                                    item.setupScreen.invoke(id,navController)
                                }
                            }
                        }
                        composable("${Screen.QuestStats.route}{baseQuestInfo}") { backStackEntry ->
                            val json = backStackEntry.arguments?.getString("baseQuestInfo")
                            val commonQuestInfo = json?.let { Json.decodeFromString<CommonQuestInfo>(it) }

                            BaseQuestStatsView(commonQuestInfo!!,navController)
                        }
                    }
                }
            }
        }
    }
}

