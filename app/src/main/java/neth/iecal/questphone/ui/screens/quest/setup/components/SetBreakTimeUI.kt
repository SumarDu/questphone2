package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SetBreakTimeUI(breakDuration: MutableState<Long>) {
    val breakInMinutes = remember {
        mutableStateOf((breakDuration.value / 60000).toString())
    }

    LaunchedEffect(breakInMinutes.value) {
        breakDuration.value = (breakInMinutes.value.toLongOrNull() ?: 0) * 60000
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Break Time",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TimeInput(
            label = "Minutes",
            time = breakInMinutes
        )
    }
}
