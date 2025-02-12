package launcher.launcher.ui.screens.quest.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.withContext
import launcher.launcher.models.AppInfo
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.quest.integration.components.SetFocusTimeUI
import launcher.launcher.utils.getCachedApps


@Composable
fun SetFocusIntegration(
    navController: NavController
) {
    val context = LocalContext.current
    val apps = getCachedApps(context)

    var selectedApps by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        floatingActionButton = {
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between buttons
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(text = "Previous")
                }

                Button(
                    onClick = {
                        navController.popBackStack()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(text = "Next")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp) // Consistent padding
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SetFocusTimeUI()
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "Select Unrestricted Apps",
                        modifier = Modifier.padding(bottom = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                items(apps) { appInfo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedApps = if (selectedApps.contains(appInfo.packageName)) {
                                    selectedApps - appInfo.packageName
                                } else {
                                    selectedApps + appInfo.packageName
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedApps.contains(appInfo.packageName),
                            onCheckedChange = { isChecked ->
                                selectedApps = if (isChecked) selectedApps + appInfo.packageName else selectedApps - appInfo.packageName
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
    }
}
