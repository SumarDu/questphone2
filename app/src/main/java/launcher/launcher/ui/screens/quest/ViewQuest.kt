package launcher.launcher.ui.screens.quest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import launcher.launcher.config.Integration
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.utils.QuestHelper

@Composable
fun ViewQuest(
    navController: NavHostController,
    basicQuestInfo: BasicQuestInfo
) {
    val context = LocalContext.current
    val questHelper = QuestHelper(context)

    val showDestroyQuestDialog = remember { mutableStateOf(false) }

    Surface {
        if(QuestHelper.isNeedAutoDestruction(basicQuestInfo)){
            showDestroyQuestDialog.value =true
        }else{
            Integration.viewScreens[basicQuestInfo.integrationId.name]?.invoke(basicQuestInfo)
        }
        if(showDestroyQuestDialog.value) DestroyQuestDialog {
            questHelper.markAsDestroyed(basicQuestInfo.title)
            showDestroyQuestDialog.value =false
            navController.navigate(Screen.HomeScreen.route) {
                popUpTo(Screen.ViewQuest.route) { inclusive = true }
            }
        }
    }

}

@Composable
fun DestroyQuestDialog(onDismiss: () -> Unit) {

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This Quest has been destroyed.....",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }

}