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
import launcher.launcher.ui.navigation.AddNewQuestSubScreens
import launcher.launcher.utils.getCachedApps


@Composable
fun SetAppFocusIntegration(
    previousScreen: MutableState<String>,
    nextScreen: MutableState<String>,
    isBackButtonFinish: MutableState<Boolean>,
    isNextEnabled: MutableState<Boolean>,

    selectedApp: MutableState<String>
) {

    previousScreen.value = AddNewQuestSubScreens.QuestInfo.route
    nextScreen.value = AddNewQuestSubScreens.FocusDuration.route
    isBackButtonFinish.value = false
    isNextEnabled.value = selectedApp.value.isNotEmpty()

    val apps = getCachedApps(LocalContext.current)

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 32.dp),
        text = "App Focus",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {

        item {
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
