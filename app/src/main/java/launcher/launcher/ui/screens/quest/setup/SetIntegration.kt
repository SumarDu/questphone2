package launcher.launcher.ui.screens.quest.setup

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import launcher.launcher.R
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.IntegrationInfo
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.navigation.SetupQuestScreen
import launcher.launcher.ui.screens.quest.setup.components.Navigation

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetIntegration(navController: NavHostController) {
    val selectedIntegration = remember { mutableStateOf(IntegrationId.APP_FOCUS) }

    Scaffold(
        floatingActionButton = {

            Navigation(
                onBackPressed = {

                },
                onNextPressed = {
                    navController.navigate(SetupQuestScreen.DeepFocus.route)
                },
                backButtonText = "Exit",
                isNextEnabled = mutableStateOf(true),
            )


        }
    )
    { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)

        ) {
            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)

            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 32.dp),
                    text = "Select Integration",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                )

                IntegrationsList(selectedIntegration)
            }
        }

    }

}


@Composable
fun IntegrationsList(
    selectedItem: MutableState<IntegrationId>
) {
    val items = listOf(
        IntegrationInfo(R.drawable.baseline_timer_24, "Deep Focus", "Block all apps except the essential ones for a set period, allowing you to stay focused on your work.", IntegrationId.DEEP_FOCUS),
        IntegrationInfo(R.drawable.baseline_phone_android_24, "App", "Restrict access to a single app while blocking everything else.", IntegrationId.APP_FOCUS),
        IntegrationInfo(R.drawable.baseline_directions_run_24, "Health Connect", "Earn coins by doing workout.", IntegrationId.HEALTH),
        IntegrationInfo(R.drawable.baseline_extension_24, "Add", "Add more integrations")
    )


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        items(items) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedItem.value = item.id },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedItem.value = item.id }) {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label,
                        modifier = Modifier.size(50.dp),
                        tint = if (selectedItem.value == item.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (selectedItem.value == item.id) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

