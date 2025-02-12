package launcher.launcher.ui.screens.quest.integration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.ui.screens.quest.integration.components.SetFocusTimeUI


@Composable
fun SetAppFocusIntegration(
    navController: NavController
) {
    val apps = listOf("Chrome", "YouTube", "WhatsApp", "Spotify", "Telegram")
    var selectedApp by remember { mutableStateOf("") }

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
            SetFocusTimeUI()
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Select App to focus",
                modifier = Modifier.padding(bottom = 16.dp),
                fontWeight = FontWeight.Bold
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(apps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedApp = app // Only one selection allowed
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedApp == app,
                            onClick = {
                                selectedApp = app
                            }
                        )
                        Text(
                            app,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

        }
        }
    }
