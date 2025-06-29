package neth.iecal.questphone

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.services.AppBlockerService
import neth.iecal.questphone.ui.navigation.Navigator
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.navigation.SetupQuestScreen
import neth.iecal.questphone.ui.screens.launcher.HomeScreen
import neth.iecal.questphone.ui.screens.onboard.SelectApps
import neth.iecal.questphone.ui.screens.onboard.SelectAppsModes
import neth.iecal.questphone.ui.screens.pet.PetDialog
import neth.iecal.questphone.ui.screens.quest.ListAllQuests
import neth.iecal.questphone.ui.screens.quest.RewardDialogMaker
import neth.iecal.questphone.ui.screens.quest.ViewQuest
import neth.iecal.questphone.ui.screens.game.CreateAppUnlockerScreen
import neth.iecal.questphone.ui.screens.quest.setup.SetIntegration
import neth.iecal.questphone.ui.screens.settings.GestureSettingsScreen
import neth.iecal.questphone.ui.screens.settings.SettingsScreen
import neth.iecal.questphone.ui.screens.quest.stats.specific.BaseQuestStatsView
import neth.iecal.questphone.ui.theme.LauncherTheme
import neth.iecal.questphone.utils.isOnline
import neth.iecal.questphone.utils.triggerQuestSync
import neth.iecal.questphone.ui.screens.game.StoreScreen
import neth.iecal.questphone.ui.screens.launcher.AppsListScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val data = getSharedPreferences("onboard", MODE_PRIVATE)


        setContent {
            val permissionsToRequest = arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
            )

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissions ->
                    permissions.entries.forEach { (permission, isGranted) ->
                        Log.d("MainActivity", "Permission: $permission, Granted: $isGranted")
                    }
                }
            )

            LaunchedEffect(Unit) {
                launcher.launch(permissionsToRequest)
            }

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

                    val forceCurrentScreen = remember { derivedStateOf { Navigator.currentScreen } }
                    RewardDialogMaker()

                    PetDialog(
                        petId = "turtie",
                        isPetDialogVisible,
                        navController
                    )
                    LaunchedEffect(Unit) {
                        unSyncedQuestItems.collect {
                            if (context.isOnline() && !User.userInfo.isAnonymous) {
                                triggerQuestSync(applicationContext)
                            }
                        }
                        unSyncedStatsItems.collect {
                            if (context.isOnline() && !User.userInfo.isAnonymous ) {
                                triggerQuestSync(applicationContext)
                            }
                        }
                    }
                    LaunchedEffect(forceCurrentScreen.value) {
                        Log.d("MainActivity", "triggered screen change")
                        if (forceCurrentScreen.value != null) {
                            navController.navigate(forceCurrentScreen.value!!)
                            Navigator.currentScreen = null
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.HomeScreen.route,
                    ) {

                        composable(Screen.Settings.route) {
                            SettingsScreen(navController)
                        }
                        composable(Screen.GestureSettings.route) {
                            GestureSettingsScreen()
                        }
                        composable(
                            route = "${Screen.SelectApps.route}{mode}",
                            arguments = listOf(navArgument("mode") { type = NavType.IntType })
                        ) { backstack ->
                            val mode = backstack.arguments?.getInt("mode")
                            SelectApps(SelectAppsModes.entries[mode!!])
                        }
                        composable(Screen.HomeScreen.route) {
                            HomeScreen(navController)
                        }

                        composable(Screen.Store.route) {
                            StoreScreen(navController)
                        }
                        composable(Screen.CreateAppUnlocker.route) {
                            CreateAppUnlockerScreen(navController)
                        }

                        composable(Screen.ListAllQuest.route) {
                            ListAllQuests(navController)
                        }
                        composable(
                            route = "${Screen.ViewQuest.route}{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id")

                            ViewQuest(navController, id!!)
                        }

                        navigation(
                            startDestination = SetupQuestScreen.Integration.route,
                            route = Screen.AddNewQuest.route
                        ) {
                            composable(SetupQuestScreen.Integration.route) {
                                SetIntegration(
                                    navController
                                )
                            }
                            IntegrationId.entries.forEach { item ->
                                composable(
                                    route = item.name + "/{id}",
                                    arguments = listOf(navArgument("id") {
                                        type = NavType.StringType
                                    })
                                ) { backstack ->
                                    var id = backstack.arguments?.getString("id")
                                    if (id == "ntg") {
                                        id = null
                                    }
                                    item.setupScreen.invoke(id, navController)
                                }
                            }
                        }
                        composable("${Screen.QuestStats.route}{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id")

                            BaseQuestStatsView(id!!, navController)
                        }
                        composable(Screen.AppList.route) {
                            AppsListScreen(navController)
                        }
                    }
                }
            }
        }
    }
}

