package launcher.launcher.ui.screens.quest.setup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import launcher.launcher.ui.navigation.Screen

@Composable
fun Navigation(onBackPressed: ()->Unit, onNextPressed: ()->Unit, backButtonText: String = "Back", proceedButtonText: String = "Next", isNextEnabled: MutableState<Boolean>){
    Row(
        modifier = Modifier.padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onBackPressed() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)

        ) {
            Text(text = backButtonText)
        }
        Button(
            onClick = {
                onNextPressed()
            },
            enabled = isNextEnabled.value,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(text = proceedButtonText)
        }
    }
}