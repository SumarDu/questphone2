package neth.iecal.questphone.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import neth.iecal.questphone.data.quest.QuestDatabaseProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAppUnlockerScreen(
    navController: NavHostController,
    viewModel: CreateAppUnlockerViewModel = viewModel(
        factory = CreateAppUnlockerViewModelFactory(
            QuestDatabaseProvider.getInstance(LocalContext.current).appUnlockerItemDao()
        )
    )
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadApps(context)
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create New App Unlocker", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // App Selector
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                readOnly = true,
                value = viewModel.selectedApp?.name ?: "Select an app",
                onValueChange = {},
                label = { Text("Application") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (viewModel.apps.isEmpty()) {
                     DropdownMenuItem(
                        text = { Text("Loading apps...") },
                        onClick = { },
                        enabled = false
                    )
                }
                viewModel.apps.forEach { appInfo ->
                    DropdownMenuItem(
                        text = { Text(appInfo.name) },
                        onClick = {
                            viewModel.selectedApp = appInfo
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Price
        OutlinedTextField(
            value = viewModel.price,
            onValueChange = { viewModel.price = it },
            label = { Text("Price (Coins)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Duration
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = viewModel.hours,
                onValueChange = { viewModel.hours = it },
                label = { Text("Hours") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = viewModel.minutes,
                onValueChange = { viewModel.minutes = it },
                label = { Text("Minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.saveAppUnlocker {
                    navController.popBackStack()
                }
            },
            enabled = viewModel.selectedApp != null && viewModel.price.isNotBlank() && (viewModel.hours.isNotBlank() || viewModel.minutes.isNotBlank())
        ) {
            Text("Save App Unlocker")
        }
    }
}
