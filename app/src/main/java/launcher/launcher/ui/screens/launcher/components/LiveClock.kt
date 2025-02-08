package launcher.launcher.ui.screens.launcher.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import launcher.launcher.ui.theme.JetBrainsMonoFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveClock(modifier: Modifier) {
    var time by remember { mutableStateOf(getCurrentTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            time = getCurrentTime()
            val delayMillis = 60_000 - (System.currentTimeMillis() % 60_000) // Delay until next minute
            delay(delayMillis)
        }
    }

    Text(
        style = MaterialTheme.typography.headlineLarge,
        text = getCurrentTime(),
        fontFamily = JetBrainsMonoFont,
        modifier = modifier
    )
}

fun getCurrentTime(): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) // HH:MM AM/PM format
}
