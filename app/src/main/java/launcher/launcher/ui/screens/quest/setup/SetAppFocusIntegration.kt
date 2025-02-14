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
import launcher.launcher.ui.navigation.QuestSetupScreen
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.quest.setup.components.Navigation
import launcher.launcher.ui.screens.quest.setup.components.SetFocusTimeUI
import launcher.launcher.utils.getCachedApps


@Composable
fun SetAppFocusIntegration(
    previousScreen: MutableState<String>,
    nextScreen: MutableState<String>,
    isBackButtonFinish: MutableState<Boolean>,

    selectedApp: MutableState<String>
) {

    previousScreen.value = QuestSetupScreen.QuestInfo.route
    nextScreen.value = "finish"
    isBackButtonFinish.value = false
    val apps = getCachedApps(LocalContext.current)
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SetFocusTimeUI()
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
