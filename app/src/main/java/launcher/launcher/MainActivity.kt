package launcher.launcher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.QuestDatabaseProvider
import launcher.launcher.data.quest.stats.StatsDatabaseProvider
import launcher.launcher.services.AppBlockerService
import launcher.launcher.ui.navigation.Navigator
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.navigation.SetupQuestScreen
import launcher.launcher.ui.screens.game.StoreScreen
import launcher.launcher.ui.screens.game.UserInfoScreen
import launcher.launcher.ui.screens.launcher.AppList
import launcher.launcher.ui.screens.launcher.HomeScreen
import launcher.launcher.ui.screens.onboard.SelectApps
import launcher.launcher.ui.screens.onboard.SelectAppsModes
import launcher.launcher.ui.screens.pet.PetDialog
import launcher.launcher.ui.screens.quest.ListAllQuests
import launcher.launcher.ui.screens.quest.RewardDialogMaker
import launcher.launcher.ui.screens.quest.ViewQuest
import launcher.launcher.ui.screens.quest.setup.SetIntegration
import launcher.launcher.ui.screens.quest.stats.specific.BaseQuestStatsView
import launcher.launcher.ui.theme.LauncherTheme
import launcher.launcher.utils.isOnline
import launcher.launcher.utils.triggerQuestSync


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val data = getSharedPreferences("onboard", MODE_PRIVATE)


        setContent {
            val isUserOnboarded = remember {mutableStateOf(true)}
            val isPetDialogVisible = remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                isUserOnboarded.value = data.getBoolean("onboard",false)
                Log.d("onboard", isUserOnboarded.value.toString())

                if(isUserOnboarded.value){
                    startForegroundService(Intent(this@MainActivity, AppBlockerService::class.java))
                }else{
                    val intent = Intent(this@MainActivity, OnboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            LauncherTheme {
                Surface {
                    val navController = rememberNavController()
                    val currentRoute = navController.currentBackStackEntryAsState()

                    val questDao = QuestDatabaseProvider.getInstance(applicationContext).questDao()
                    val statsDao = StatsDatabaseProvider.getInstance(applicationContext).statsDao()

                    val unSyncedQuestItems = remember { questDao.getUnSyncedQuests() }
                    val unSyncedStatsItems = remember { statsDao.getAllUnSyncedStats() }
                    val context = LocalContext.current

                    val forceCurrentScreen = remember {derivedStateOf { Navigator.currentScreen }}
                    RewardDialogMaker()

                    PetDialog(
                        petId = "turtie",
                        isPetDialogVisible,
                        navController
                    )
                    LaunchedEffect(Unit) {
                        unSyncedQuestItems.collect {
                            if(context.isOnline()){
                                triggerQuestSync(applicationContext)
                            }
                        }
                        unSyncedStatsItems.collect {
                            if(context.isOnline()){
                                triggerQuestSync(applicationContext)
                            }
                        }
                    }
                    LaunchedEffect(forceCurrentScreen.value) {
                        Log.d("MainActivity","triggered screen change")
                        if(forceCurrentScreen.value!=null){
                            navController.navigate(forceCurrentScreen.value!!)
                            Navigator.currentScreen = null
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.HomeScreen.route,
                    ) {

                        composable(Screen.UserInfo.route) {
                            UserInfoScreen()
                        }
                        composable( route = "${Screen.SelectApps.route}{mode}",
                            arguments = listOf(navArgument("mode") { type = NavType.IntType })) { backstack ->
                            val mode =  backstack.arguments?.getInt("mode")
                            SelectApps(SelectAppsModes.entries[mode!!])
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
                            route = "${Screen.ViewQuest.route}{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id")

                            ViewQuest(navController,id!!)
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
                        composable("${Screen.QuestStats.route}{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id")

                            BaseQuestStatsView(id!!,navController)
                        }
                    }
                }
            }
        }
    }
}

