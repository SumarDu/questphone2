package launcher.launcher.ui.screens.launcher.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProgressBar(modifier: Modifier, progress: Int) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
    ) {
        // Background
        drawRect(
            color = backgroundColor,
            size = size
        )
        // Progress (20%)
        drawRect(
            color = progressColor,
            size = size.copy(width = size.width * progress)
        )
    }
}
