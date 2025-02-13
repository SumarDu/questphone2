package launcher.launcher.ui.screens.quest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import launcher.launcher.Constants
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.quest.components.InstructionsList
import launcher.launcher.ui.screens.quest.components.IntegrationsList
import launcher.launcher.ui.screens.quest.setup.components.Navigation

@Composable
fun SetIntegration(
    navController: NavController,
) {
    val selectedItem = remember { mutableStateOf<Int?>(Constants.INTEGRATION_ID_APP_FOCUS) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {

            Navigation(onNextPressed = {
                when(selectedItem.value){
                    Constants.INTEGRATION_ID_FOCUS ->
                        navController.navigate(Screen.SetFocusIntegration.route)
                    Constants.INTEGRATION_ID_APP_FOCUS ->
                        navController.navigate(Screen.SetAppFocusIntegration.route)
                }
                                       },
                onBackPressed = { navController.popBackStack() },
                isBackButtonHidden = true)
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            horizontalAlignment = Alignment.Start,
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 32.dp),
                    text = "Integration",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )

                IntegrationsList(selectedItem)
            }
        }

    }
}