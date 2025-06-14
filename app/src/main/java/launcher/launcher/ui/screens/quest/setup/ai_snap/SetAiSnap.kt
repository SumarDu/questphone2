package launcher.launcher.ui.screens.quest.setup.ai_snap

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import launcher.launcher.data.IntegrationId
import launcher.launcher.data.quest.QuestDatabaseProvider
import launcher.launcher.data.quest.QuestInfoState
import launcher.launcher.data.quest.ai.snap.AiSnap
import launcher.launcher.ui.screens.quest.setup.ReviewDialog
import launcher.launcher.ui.screens.quest.setup.components.SetBaseQuest
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.json
import java.io.File
import java.io.FileOutputStream

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetAiSnap(editQuestId:String? = null,navController: NavHostController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val sp = QuestHelper(LocalContext.current)

    // State for the quest
    val questInfoState = remember { QuestInfoState(initialIntegrationId = IntegrationId.AI_SNAP) }
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
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if(editQuestId!=null){
            val dao = QuestDatabaseProvider.getInstance(context).questDao()
            val quest = dao.getQuest(editQuestId)
            questInfoState.fromBaseQuest(quest!!)
            val aiSnap = json.decodeFromString<AiSnap>(quest.quest_json)
            taskDescription.value = aiSnap.taskDescription
//            spatialImageUri.value = aiSnap.spatialImageUrl
            TODO("FIX Ts")
        }
    }

    // Review dialog before creating the quest
    if (isReviewDialogVisible.value) {
        val aiSnapQuest = AiSnap(
            taskDescription = taskDescription.value,
        )
        val baseQuest = questInfoState.toBaseQuest(aiSnapQuest)

        ReviewDialog(
            items = listOf(baseQuest, aiSnapQuest),
            onConfirm = {
                scope.launch {
                    val dao = QuestDatabaseProvider.getInstance(context).questDao()
                    dao.upsertQuest(baseQuest)
                }
                isReviewDialogVisible.value = false
                navController.popBackStack()
                if(spatialImageUri.value !=null){
                    aiSnapQuest.spatialImageUrl = saveImageToLocalFiles(context,spatialImageUri.value!!)?.absolutePath
                }
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
                    SetBaseQuest(questInfoState)

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
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(context)
                                                .data(spatialImageUri.value)
                                                .build()
                                        ),
                                        contentDescription = "Reference Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
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
private fun saveImageToLocalFiles(context: Context, uri: Uri): File? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val directory = File(context.filesDir, "images")
        if (!directory.exists()) directory.mkdirs()

        val file = File(directory, "saved_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)

        inputStream.use { input -> outputStream.use { output ->
            input.copyTo(output)
        } }

        return file // Returns the saved file
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
