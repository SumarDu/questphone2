package launcher.launcher.ui.screens.quest.setup.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import launcher.launcher.data.quest.BaseQuestState
import launcher.launcher.utils.QuestHelper

@Composable
fun SetBaseQuest(baseQuestState: BaseQuestState,) {

//        Text(
//            text = "Set Info",
//            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
//            modifier = Modifier.fillMaxWidth()
//        )

    val sp = QuestHelper(LocalContext.current)
    val allQuestTitles = mutableSetOf<String>()

    var isTitleDublicate by remember { mutableStateOf(false) }

    LaunchedEffect(allQuestTitles) {
        allQuestTitles.addAll(
            sp.getQuestList().map { it.title }
        )

    }

    OutlinedTextField(
        value = baseQuestState.title,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = {
            isTitleDublicate = allQuestTitles.contains(it)
            baseQuestState.title = it
                        },
        label = { Text("Quest Title") },
        modifier = Modifier.fillMaxWidth(),
        isError = isTitleDublicate
    )
    if(isTitleDublicate){
        Text(text = "Title already exists", color = MaterialTheme.colorScheme.error)
    }

    SelectDaysOfWeek(baseQuestState)


    OutlinedTextField(
        value = baseQuestState.instructions,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = { baseQuestState.instructions = it }, // Direct update
        label = { Text("Instructions") },
        modifier = Modifier.fillMaxWidth()
            .height(200.dp)
    )

}
