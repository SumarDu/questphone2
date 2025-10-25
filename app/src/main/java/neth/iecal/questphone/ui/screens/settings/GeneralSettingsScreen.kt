package neth.iecal.questphone.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HourglassDisabled
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import neth.iecal.questphone.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            SettingsSectionCard(
                items = listOf(
                    SettingItemData(
                        icon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "Checkpoints",
                        subtitle = "Set up and manage quest milestones",
                        onClick = { navController.navigate(Screen.SettingsCheckpoints.route) }
                    ),
                    SettingItemData(
                        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "Protection",
                        subtitle = "Secure your app and progress",
                        onClick = { navController.navigate(Screen.SettingsProtection.route) }
                    ),
                    SettingItemData(
                        icon = { Icon(Icons.Default.HourglassDisabled, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "Overdue penalties",
                        subtitle = "Define consequences for late tasks",
                        onClick = { navController.navigate(Screen.SettingsOverduePenalties.route) }
                    ),
                    SettingItemData(
                        icon = { Icon(Icons.Default.Coffee, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "Unplanned Break Reasons",
                        subtitle = "Customize your break options",
                        onClick = { navController.navigate(Screen.SettingsUnplannedBreakReasons.route) }
                    ),
                    SettingItemData(
                        icon = { Icon(Icons.Default.FilterAlt, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "Unplanned Quest Filter",
                        subtitle = "Manage visibility of unplanned quests",
                        onClick = { navController.navigate(Screen.SettingsUnplannedQuestFilter.route) }
                    ),
                    SettingItemData(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "Gestures",
                        subtitle = "Configure shortcuts and actions",
                        onClick = { navController.navigate(Screen.GestureSettings.route) }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSectionCard(
                items = listOf(
                    SettingItemData(
                        icon = { Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "Calendar Sync Settings",
                        subtitle = "Connect and manage your calendar",
                        onClick = { navController.navigate(Screen.SettingsCalendarSync.route) }
                    ),
                    SettingItemData(
                        icon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "AI tools settings",
                        subtitle = "Configure your AI assistants",
                        onClick = { navController.navigate(Screen.SettingsAiTools.route) }
                    ),
                    SettingItemData(
                        icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "Backups & Development",
                        subtitle = "Manage data and developer options",
                        onClick = { navController.navigate(Screen.SettingsBackupsDev.route) }
                    ),
                    SettingItemData(
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color(0xFF8E8E93)) },
                        title = "Other",
                        subtitle = "Additional settings and options",
                        onClick = { navController.navigate(Screen.SettingsOther.route) }
                    )
                )
            )
        }
    }
}

private data class SettingItemData(
    val icon: @Composable () -> Unit,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
private fun SettingsSectionCard(items: List<SettingItemData>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        items.forEachIndexed { index, item ->
            SettingRow(item)
            if (index != items.lastIndex) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun SettingRow(item: SettingItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.icon()
        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 16.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF8E8E93))
    }
}
