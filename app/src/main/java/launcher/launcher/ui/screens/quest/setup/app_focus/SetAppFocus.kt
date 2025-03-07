package launcher.launcher.ui.screens.quest.setup.app_focus

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
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.focus.DeepFocus
import launcher.launcher.data.quest.focus.FocusTimeConfig
import launcher.launcher.data.quest.BaseQuestState
import launcher.launcher.data.quest.focus.AppFocus
import launcher.launcher.ui.screens.quest.setup.ReviewDialog
import launcher.launcher.ui.screens.quest.setup.components.SetBaseQuest
import launcher.launcher.ui.screens.quest.setup.components.SetFocusTimeUI
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.getCachedApps

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetAppFocus() {
    val context = LocalContext.current
    val apps = remember { getCachedApps(context).map { it.name to it.packageName } }

    val showDialog = remember { mutableStateOf(false) }
    val selectedApp = remember { mutableStateOf("") }
    val baseQuestState = remember { BaseQuestState(initialIntegrationId = IntegrationId.DEEP_FOCUS) }
    val focusTimeConfig = remember { mutableStateOf(FocusTimeConfig()) }

    val scrollState = rememberScrollState()
    val sp = QuestHelper(LocalContext.current)

    val isReviewDialogVisible = remember { mutableStateOf(false) }


    if (showDialog.value) {
        SelectAppDialog(
            apps = apps,
            selectedApp = selectedApp,
            onDismiss = { showDialog.value = false }
        )
    }
    if (isReviewDialogVisible.value) {
        ReviewDialog(
            items = listOf(
                baseQuestState.toBaseQuest(), AppFocus(
                    focusTimeConfig = focusTimeConfig.value,
                    selectedFocusApp = selectedApp.value

                )
            ),

            onConfirm = {
                sp.appendToQuestList(
                    baseQuestState.toBaseQuest(), AppFocus(
                        focusTimeConfig = focusTimeConfig.value,
                        selectedFocusApp = selectedApp.value
                    )
                )
                isReviewDialogVisible.value = false
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
                        text = "App Focus",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    SetBaseQuest(baseQuestState)

                    OutlinedButton(
                        onClick = { showDialog.value = true },
                    ) {
                        Text(
                            text = "Select Focus App",
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