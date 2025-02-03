package launcher.launcher.ui.screens.launcher.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun QuestItem(
    text: String,
    isCompleted: Boolean = false,
    modifier: Modifier
) {
    Text(
        text = text,
        color = if (isCompleted) Color.Gray else Color.White,
        style = if (isCompleted) {
            MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
        } else {
            MaterialTheme.typography.bodyLarge
        },
        modifier = modifier.fillMaxWidth()
            .padding(8.dp),
        textAlign = TextAlign.Center
    )
}