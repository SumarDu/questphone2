package launcher.launcher.ui.screens.quest.view

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.launch
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.data.quest.health.HealthQuest
import launcher.launcher.data.quest.health.HealthTaskType
import launcher.launcher.data.quest.health.formatHealthData
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.HealthConnectManager
import launcher.launcher.utils.HealthConnectPermissionManager
import launcher.launcher.utils.QuestHelper

@Composable
fun HealthQuestView(baseQuestInfo: BasicQuestInfo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val questHelper = QuestHelper(context)
    val healthQuest = questHelper.getQuestInfo<HealthQuest>(baseQuestInfo) ?: return

    val healthManager = HealthConnectManager(context)
    val permissionManager = HealthConnectPermissionManager(context)

    val hasRequiredPermissions = remember { mutableStateOf(false) }
    val currentHealthData = remember { mutableDoubleStateOf(0.0) }
    val progressState = remember { mutableFloatStateOf(0f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = permissionManager.requestPermissionContract,
        onResult = { granted ->
            hasRequiredPermissions.value = granted.containsAll(permissionManager.permissions)
            if (hasRequiredPermissions.value) {
                scope.launch {
                    fetchHealthData(healthManager, healthQuest.type) { data ->
                        currentHealthData.doubleValue = data
                        // Update progress based on nextGoal
                        progressState.floatValue = (data / healthQuest.nextGoal).toFloat().coerceIn(0f, 1f)
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        val isHealthConnectAvailable = healthManager.isAvailable()
        if (!isHealthConnectAvailable) {
            Log.d("HealthConnect", "Health Connect not available")
            return@LaunchedEffect
        }

        hasRequiredPermissions.value = healthManager.hasAllPermissions()
        if (!hasRequiredPermissions.value) {
            permissionLauncher.launch(permissionManager.permissions)
        } else {
            fetchHealthData( healthManager, healthQuest.type) { data ->
                currentHealthData.doubleValue = data
                progressState.floatValue = (data / healthQuest.nextGoal).toFloat().coerceIn(0f, 1f)
            }
        }
    }

    BaseQuestView(
        hideStartQuestBtn = true,
        progress = progressState,
        onQuestStarted = { /* No-op for now, health quests auto-track */ }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!hasRequiredPermissions.value) {
                Text(
                    text = "Health Connect permissions required",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = { permissionLauncher.launch(permissionManager.permissions) },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Grant Permissions")
                }
            } else {
                Text(
                    text = baseQuestInfo.title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    fontFamily = JetBrainsMonoFont,
                    modifier = Modifier.padding(top = 40.dp)
                )
                Text(
                    text = "Reward: ${baseQuestInfo.reward} coins",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )
                Text(
                    text = "Current Progress: ${formatHealthData(healthQuest.type, currentHealthData.doubleValue)} / ${healthQuest.nextGoal}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
    }
}

private suspend fun fetchHealthData(
    healthManager: HealthConnectManager,
    taskType: HealthTaskType,
    onDataReceived: (Double) -> Unit
) {
    try {
        val data = healthManager.getTodayHealthData(taskType)
        Log.d("HealthConnect", "Fetched data for $taskType: $data")
        onDataReceived(data)
    } catch (e: Exception) {
        Log.e("HealthConnect", "Error fetching health data: ${e.message}", e)
        onDataReceived(0.0) // Fallback to 0 on error
    }
}