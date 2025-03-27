package launcher.launcher.ui.screens.quest.view.ai_snap

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import launcher.launcher.utils.ai.TaskValidationClient

import java.io.File
@Composable
fun AiEvaluationScreen(
    isAiEvaluating: MutableState<Boolean>,
    description: String,
    onEvaluationComplete: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val client = remember { TaskValidationClient() }
    val photoFile = File(context.getExternalFilesDir(null), "ai_snap_captured_image.jpg")

    // State variables
    var isLoading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<TaskValidationClient.ValidationResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Trigger validation immediately
    LaunchedEffect(Unit) {
        if (!photoFile.exists()) {
            error = "Image file not found at ${photoFile.absolutePath}"
            isLoading = false
            return@LaunchedEffect
        }
        client.validateTask(photoFile, description) { response ->
            isLoading = false
            response.fold(
                onSuccess = { validationResult ->
                    result = validationResult
                    onEvaluationComplete(validationResult.isValid)
                },
                onFailure = { e ->
                    error = e.message
                    onEvaluationComplete(false) // Treat error as failure
                }
            )
        }
    }

    // Show toast for errors
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            error = null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {

        // Image Section
        if (photoFile.exists()) {
            ScanningImageCard(
                photoFile = photoFile,
                isScanning = isLoading,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Status Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            when {
                isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Evaluating...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                result != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result!!.isValid)
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else
                                Color(0xFFFF9800).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = if (result!!.isValid) "Valid" else "Invalid",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (result!!.isValid) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result!!.reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = { isAiEvaluating.value = false }) {
                                Text(text = if(result!!.isValid) "Close" else "Retake")
                            }
                        }
                    }
                }
                error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = launcher.launcher.R.drawable.baseline_error_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = { isAiEvaluating.value = false }) {
                                Text(text = "Close")
                            }

                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanningImageCard(
    photoFile: File,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val scanAnimation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanning"
    )

    val scanBrush = remember(scanAnimation.value) {
        Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                primary,
                Color.Transparent,
            ),
            start = Offset(0f, 0f),
            end = Offset(100f, 100f)
        )
    }

    Box(
        modifier = modifier
            .size(250.dp)
    ) {
        // Main Image
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            val painter = rememberAsyncImagePainter(
                model = photoFile,
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painter,
                contentDescription = "Captured Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Scanning Animation Overlay
        if (isScanning) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // Calculate the position of the scanning line
                val scanLineY = size.height * scanAnimation.value

                drawRect(
                    brush = scanBrush,
                    topLeft = Offset(0f, scanLineY - 100),
                    size = Size(size.width, 200f)
                )
            }

            // Add a subtle pulsating overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.1f * (1f - scanAnimation.value)
                        )
                    )
            )
        }
    }
}