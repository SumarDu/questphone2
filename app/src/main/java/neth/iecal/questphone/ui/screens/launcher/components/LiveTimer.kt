package neth.iecal.questphone.ui.screens.launcher.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.ui.screens.launcher.TimerViewModel

@Composable
fun LiveTimer(
    modifier: Modifier = Modifier,
    timerViewModel: TimerViewModel = viewModel()
) {
    val timerText by timerViewModel.timerText.collectAsState()
    val timerMode by timerViewModel.timerMode.collectAsState()

    val timerColor = when (timerMode) {
        TimerMode.QUEST_COUNTDOWN -> MaterialTheme.colorScheme.primary
        TimerMode.OVERTIME -> MaterialTheme.colorScheme.error
        TimerMode.BREAK -> Color.Green // Example color
        TimerMode.UNSCHEDULED_BREAK -> Color.Yellow // Example color
        TimerMode.INACTIVE -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = timerText,
        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
        color = timerColor,
        modifier = modifier
    )
}
