package launcher.launcher.ui.screens.quest.setup.deep_focus

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import launcher.launcher.data.AppInfo
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.focus.DeepFocus
import launcher.launcher.data.quest.focus.FocusTimeConfig
import launcher.launcher.data.quest.BaseQuestState
import launcher.launcher.data.quest.QuestDatabaseProvider
import launcher.launcher.ui.screens.quest.setup.ReviewDialog
import launcher.launcher.ui.screens.quest.setup.components.SetBaseQuest
import launcher.launcher.ui.screens.quest.setup.components.SetFocusTimeUI
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.getCachedApps
import launcher.launcher.utils.reloadApps

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetDeepFocus(navController: NavHostController) {
    val context = LocalContext.current
    val apps = remember { mutableStateOf(emptyList<AppInfo>()) }

    val showDialog = remember { mutableStateOf(false) }
    val selectedApps = remember { mutableStateListOf<String>() }
    val baseQuestState = remember { BaseQuestState(initialIntegrationId = IntegrationId.DEEP_FOCUS) }
    val focusTimeConfig = remember { mutableStateOf(FocusTimeConfig()) }

    val scrollState = rememberScrollState()
    val sp = QuestHelper(LocalContext.current)

    val isReviewDialogVisible = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(apps) {
        apps.value = reloadApps(context.packageManager,context).getOrNull() ?: emptyList()
    }

    if (showDialog.value) {
        SelectAppsDialog(
            apps = apps,
            selectedApps = selectedApps,
            onDismiss = {
                showDialog.value = false
            }
        )
    }
    if (isReviewDialogVisible.value) {
        val deepFocus = DeepFocus(
            focusTimeConfig = focusTimeConfig.value,
            unrestrictedApps = selectedApps.toSet(),
            nextFocusDurationInMillis = focusTimeConfig.value.initialTimeInMs
        )
        val baseQuest =
            baseQuestState.toBaseQuest<DeepFocus>(deepFocus)

        ReviewDialog(
            items = listOf(
                baseQuest,deepFocus
            ),

            onConfirm = {
                scope.launch {
                    val dao = QuestDatabaseProvider.getInstance(context).questDao()
                    dao.upsertQuest(baseQuest)
                }
                isReviewDialogVisible.value = false
                navController.popBackStack()
            },
            onDismiss = {
                isReviewDialogVisible.value = false
            }
        )
    }
    Scaffold()
    { paddingValues ->

        Box(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)

            ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(top = 32.dp)
                ) {

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = "Deep Focus ",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    SetBaseQuest(baseQuestState)

                    OutlinedButton(
                        onClick = { showDialog.value = true },
                    ) {

                        Text(
                            text = "Selected App Exceptions ${selectedApps.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    SetFocusTimeUI(focusTimeConfig)

                    Button(
                        onClick = {
                            isReviewDialogVisible.value = true

                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Done"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Quest",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Spacer(Modifier.size(100.dp))
                }

            }
        }


    }

}