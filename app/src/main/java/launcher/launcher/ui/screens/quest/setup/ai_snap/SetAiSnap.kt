package launcher.launcher.ui.screens.quest.setup.ai_snap

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.BaseQuestState
import launcher.launcher.data.quest.ai.snap.AiSnap
import launcher.launcher.ui.screens.quest.setup.ReviewDialog
import launcher.launcher.ui.screens.quest.setup.components.SetBaseQuest
import launcher.launcher.utils.QuestHelper

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetAiSnap() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val sp = QuestHelper(LocalContext.current)

    // State for the quest
    val baseQuestState = remember { BaseQuestState(initialIntegrationId = IntegrationId.AI_SNAP) }
    val taskDescription = remember { mutableStateOf("") }
    val spatialImageUri = remember { mutableStateOf<Uri?>(null) }

    // Dialog state
    val isReviewDialogVisible = remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { spatialImageUri.value = it }
    }


    // Review dialog before creating the quest
    if (isReviewDialogVisible.value) {
        val baseQuest = baseQuestState.toBaseQuest()
        val aiSnapQuest = AiSnap(
            taskDescription = taskDescription.value,
        )

        ReviewDialog(
            items = listOf(baseQuest, aiSnapQuest),
            onConfirm = {
                sp.saveInstruction(baseQuest.title,taskDescription.value)
                sp.appendToQuestList(baseQuest, aiSnapQuest)
                isReviewDialogVisible.value = false
            },
            onDismiss = {
                isReviewDialogVisible.value = false
            }
        )
    }

    Scaffold { paddingValues ->
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AI Snap",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        )

                    }

                    // Base quest configuration
                    SetBaseQuest(baseQuestState)

                    // Task description
                    OutlinedTextField(
                        value = taskDescription.value,
                        onValueChange = { taskDescription.value = it },
                        label = { Text("Task Description") },
                        placeholder = { Text("e.g., Clean the bedroom, Organize desk") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )


                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Reference Image (Optional)",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "Add a reference image of the space to ensure verification happens in the correct location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )


                        // Image preview or placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (spatialImageUri.value != null) {
                                // Show selected image
//                                    Image(
//                                        painter = rememberAsyncImagePainter(
//                                            ImageRequest.Builder(context)
//                                                .data(spatialImageUri.value)
//                                                .build()
//                                        ),
//                                        contentDescription = "Reference Image",
//                                        modifier = Modifier.fillMaxSize(),
//                                        contentScale = ContentScale.Crop
//                                    )
                            } else {
                                // Show placeholder
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Photo",
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Tap to add reference image",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }

                        if (spatialImageUri.value != null) {
                            TextButton(
                                onClick = { spatialImageUri.value = null },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Remove Image")
                            }
                        }
                    }
                }

                // Create Quest button
                Button(
                    onClick = {
                        if (taskDescription.value.isNotBlank()) {
                            isReviewDialogVisible.value = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = taskDescription.value.isNotBlank()
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