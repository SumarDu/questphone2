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
import launcher.launcher.data.IntegrationId
import launcher.launcher.config.Integration
import launcher.launcher.ui.screens.quest.setup.components.Navigation

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetIntegration(navController: NavHostController) {
    val selectedIntegration = remember { mutableStateOf(IntegrationId.DEEP_FOCUS) }

    Scaffold(
        floatingActionButton = {

            Navigation(
                onBackPressed = {
                },
                onNextPressed = {
                    Integration.setupRoutes[selectedIntegration.value.name]?.let {
                        navController.navigate(
                            it.first)
                    }
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
    val items = Integration.allInfo


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


@Composable
fun IntegrationsList(
    onSelected: (IntegrationId)-> Unit
) {
    val items = Integration.allInfo


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        items(items) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(item.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSelected(item.id) }) {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal
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

