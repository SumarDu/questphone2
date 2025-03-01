package launcher.launcher.ui.screens.quest.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.ui.navigation.AddNewQuestSubScreens
import launcher.launcher.ui.screens.quest.setup.components.IntegrationsList

@Composable
fun SetIntegration(
    previousScreen: MutableState<String>,
    nextScreen: MutableState<String>,
    isBackButtonFinish: MutableState<Boolean>,
    isNextEnabled: MutableState<Boolean>,
    selectedIntegration: MutableState<Int?>
) {

    previousScreen.value = "finish"
    nextScreen.value = AddNewQuestSubScreens.QuestInfo.route
    isBackButtonFinish.value = true
    isNextEnabled.value = true

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 32.dp),
        text = "Select Integration",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    )

    IntegrationsList(selectedIntegration)
}
