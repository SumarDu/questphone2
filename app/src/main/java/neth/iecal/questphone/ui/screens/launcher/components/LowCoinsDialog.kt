package neth.iecal.questphone.ui.screens.launcher.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController

@Composable
fun LowCoinsDialog(
    coins: Int,
    onDismiss: () -> Unit,
    appName : String,
    navController: NavController
) {
    var isPerformAQuestDialogVisible = remember { mutableStateOf(false) }

    if (isPerformAQuestDialogVisible.value) {
        QuestDialog(navController = navController) {
            isPerformAQuestDialogVisible.value = false
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Balance: $coins coins",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "You're too broke to use $appName right now. ",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.size(12.dp))

                OutlinedButton(
                    onClick = {
                        isPerformAQuestDialogVisible.value = true
                    },
                ) {
                    Text("Start A Quest")
                }

            }
        }
    }
}