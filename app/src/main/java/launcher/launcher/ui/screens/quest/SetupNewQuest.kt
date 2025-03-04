package launcher.launcher.ui.screens.quest

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import launcher.launcher.Constants
import launcher.launcher.data.DayOfWeek
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.BaseQuest
import launcher.launcher.data.quest.AppFocus
import launcher.launcher.data.quest.DeepFocus
import launcher.launcher.data.quest.FocusTimeConfig
import launcher.launcher.ui.navigation.AddNewQuestSubScreens
import launcher.launcher.ui.screens.quest.setup.ReviewFinalSettings
import launcher.launcher.ui.screens.quest.setup.SetAppFocusIntegration
import launcher.launcher.ui.screens.quest.setup.SetFocusIntegration
import launcher.launcher.ui.screens.quest.setup.SetFocusTimeUI
import launcher.launcher.ui.screens.quest.setup.SetIntegration
import launcher.launcher.ui.screens.quest.setup.SetQuestMetaInfo
import launcher.launcher.ui.screens.quest.setup.components.Navigation
import launcher.launcher.utils.QuestHelper

@Composable
fun SetupNewQuest(
    quitAddNew: () -> Unit
) {

    val currentScreen = remember { mutableStateOf(AddNewQuestSubScreens.Integration.route) }
    val isNavigatingForward = remember { mutableStateOf(true) }
    val selectedIntegration = remember { mutableStateOf<IntegrationId?>(IntegrationId.APP_FOCUS) }


    val questTitle = remember { mutableStateOf("") }
    val reward = remember { mutableIntStateOf(5) }
    val instructions = remember { mutableStateOf(emptyList<String>()) }
    val selectedDays = remember { mutableStateOf(emptySet<DayOfWeek>()) }

    val selectedUnrestrictedApps = remember { mutableStateOf(emptySet<String>()) }
    val selectedFocusApp = remember { mutableStateOf("") }

    val nextScreenId = remember { mutableStateOf(AddNewQuestSubScreens.QuestInfo.route) }
    val previousScreenId = remember { mutableStateOf("finish") }
    val isBackButtonFinish = remember { mutableStateOf(false) }
    val isNextEnabled = remember { mutableStateOf(true) }

    val focusTimeConfig = remember { mutableStateOf(FocusTimeConfig()) }

    val savedPreferences = LocalContext.current.getSharedPreferences("quest_ist",Context.MODE_PRIVATE)

    BackHandler {
        if (isBackButtonFinish.value) {
            quitAddNew()
        } else {
            isNavigatingForward.value = false
            currentScreen.value = previousScreenId.value
        }
    }

    Scaffold(
        floatingActionButton = {
            Navigation(
                onNextPressed = {

                    isNavigatingForward.value = true
                    currentScreen.value = nextScreenId.value
                },
                onBackPressed = {
                    if (isBackButtonFinish.value) {
                        quitAddNew()
                    } else {
                        isNavigatingForward.value = false
                        currentScreen.value = previousScreenId.value
                    }
                },
                isBackButtonFinish = isBackButtonFinish,
                isNextEnabled = isNextEnabled
            )

        }
    )
    { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp) // Consistent padding
        ) {
            AnimatedContent(
                targetState = currentScreen.value,
                transitionSpec = {
                    if (isNavigatingForward.value) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                Column {
                    when (targetScreen) {
                        AddNewQuestSubScreens.Integration.route -> SetIntegration(
                            previousScreenId,
                            nextScreenId,
                            isBackButtonFinish,
                            isNextEnabled,
                            selectedIntegration
                        )

                        AddNewQuestSubScreens.QuestInfo.route -> SetQuestMetaInfo(
                            previousScreenId,
                            nextScreenId,
                            isBackButtonFinish,
                            isNextEnabled,

                            instructions,
                            reward,
                            questTitle,
                            selectedDays,
                            selectedIntegration
                        )

                        AddNewQuestSubScreens.FocusDuration.route -> SetFocusTimeUI(
                            previousScreenId,
                            nextScreenId,
                            isBackButtonFinish,
                            selectedIntegration,
                            isNextEnabled,
                            focusTimeConfig
                        )
                        AddNewQuestSubScreens.FocusIntegration.route -> SetFocusIntegration(
                            previousScreenId,
                            nextScreenId,
                            isBackButtonFinish,
                            isNextEnabled,

                            selectedUnrestrictedApps,
                        )

                        AddNewQuestSubScreens.AppFocusIntegration.route -> SetAppFocusIntegration(
                            previousScreenId,
                            nextScreenId,
                            isBackButtonFinish,
                            isNextEnabled,

                            selectedFocusApp,
                        )

                        AddNewQuestSubScreens.ReviewQuest.route -> {
                            when(selectedIntegration.value){
                                Constants.INTEGRATION_ID_FOCUS -> previousScreenId.value = AddNewQuestSubScreens.FocusIntegration.route
                                Constants.INTEGRATION_ID_APP_FOCUS -> previousScreenId.value = AddNewQuestSubScreens.AppFocusIntegration.route
                            }
                            nextScreenId.value = AddNewQuestSubScreens.SavingDialog.route
                            ReviewFinalSettings(
                                reward,
                                selectedDays,
                                selectedIntegration,
                                selectedFocusApp,
                                selectedUnrestrictedApps,
                                focusTimeConfig
                            )
                        }

                        AddNewQuestSubScreens.SavingDialog.route -> {
                            val questHelper = QuestHelper(LocalContext.current)

                            LaunchedEffect(Unit) {
                                val baseQuest = BaseQuest(
                                    title = questTitle.value,
                                    instructions = instructions.value,
                                    selectedDays = selectedDays.value,
                                    integrationId = selectedIntegration.value ?: Constants.INTEGRATION_ID_FOCUS,
                                    reward = reward.intValue
                                )

                                when (selectedIntegration.value) {
                                    Constants.INTEGRATION_ID_FOCUS -> {
                                        val data = DeepFocus(
                                            baseQuest,
                                            focusTimeConfig.value,
                                            selectedUnrestrictedApps.value,
                                            focusTimeConfig.value.initialTimeInMs
                                        )
                                        baseQuest.integrationId = Constants.INTEGRATION_ID_FOCUS
                                        questHelper.appendFocusQuest(baseQuest, data)
                                    }

                                    Constants.INTEGRATION_ID_APP_FOCUS -> {
                                        val data = AppFocus(
                                            baseQuest,
                                            focusTimeConfig.value,
                                            selectedFocusApp.value,
                                            focusTimeConfig.value.initialTimeInMs
                                        )
                                        baseQuest.integrationId =
                                            Constants.INTEGRATION_ID_APP_FOCUS
                                        questHelper.appendFocusAppQuest(baseQuest, data)
                                    }
                                }
                                quitAddNew()
                            }
                        }
                    }

                }
            }
        }

    }
}