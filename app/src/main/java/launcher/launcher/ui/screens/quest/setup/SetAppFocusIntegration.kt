package launcher.launcher.ui.screens.quest.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.models.quest.FocusTimeConfig
import launcher.launcher.ui.navigation.AddNewQuestSubScreens
import launcher.launcher.ui.screens.quest.setup.components.SetFocusTimeUI
import launcher.launcher.utils.getCachedApps


@Composable
fun SetAppFocusIntegration(
    previousScreen: MutableState<String>,
    nextScreen: MutableState<String>,
    isBackButtonFinish: MutableState<Boolean>,
    isNextEnabled: MutableState<Boolean>,

    selectedApp: MutableState<String>,
    focusTimeConfig: MutableState<FocusTimeConfig>
) {

    previousScreen.value = AddNewQuestSubScreens.QuestInfo.route
    nextScreen.value = AddNewQuestSubScreens.ReviewQuest.route
    isBackButtonFinish.value = false
    isNextEnabled.value = selectedApp.value.isNotEmpty() && focusTimeConfig.value.finalTime.toInt()!=0 && focusTimeConfig.value.initialTime.toInt()!=0 && focusTimeConfig.value.incrementTime.toInt()!=0

    val apps = getCachedApps(LocalContext.current)

    SetFocusTimeUI(focusTimeConfig)
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Select an app to focus",
                modifier = Modifier.padding(bottom = 16.dp),
                fontWeight = FontWeight.Bold
            )
        }
        items(apps) { appInfo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedApp.value = appInfo.packageName // Only one selection allowed
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedApp.value == appInfo.packageName,
                    onClick = {
                        selectedApp.value = appInfo.packageName
                    }
                )
                Text(
                    appInfo.name,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
    }
