package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.first
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.getCurrentDay

@Composable
fun SetBaseQuest(questInfoState: QuestInfoState, isTimeRangeSupported: Boolean = true) {

    val allQuestTitles = mutableSetOf<String>()

    var isTitleDublicate by remember { mutableStateOf(false) }
    val dao = QuestDatabaseProvider.getInstance(LocalContext.current).questDao()

    LaunchedEffect(Unit) {
        allQuestTitles.addAll(
            dao.getAllQuests().first().map { it.title }
        )

    }

    OutlinedTextField(
        value = questInfoState.title,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = {
            isTitleDublicate = allQuestTitles.contains(it)
            questInfoState.title = it
                        },
        label = { Text("Quest Title") },
        modifier = Modifier.fillMaxWidth(),
        isError = isTitleDublicate
    )
    if(isTitleDublicate){
        Text(text = "Title already exists", color = MaterialTheme.colorScheme.error)
    }

    if(questInfoState.selectedDays.contains(getCurrentDay()) && User.userInfo.getCreatedOnString() != getCurrentDate()){
        Text("To prevent abuse, you can't perform this quest today. You'll be able to do it starting from the next time it occurs.")
    }
    SelectDaysOfWeek(questInfoState)


    OutlinedTextField(
        value = questInfoState.instructions,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = { questInfoState.instructions = it }, // Direct update
        label = { Text("Instructions") },
        modifier = Modifier.fillMaxWidth()
            .height(200.dp)
    )
    AutoDestruct(questInfoState)

    if(isTimeRangeSupported){
        SetTimeRange(questInfoState)
    }

    RewardSetter(questInfoState)

}

@Composable
fun RewardSetter(questInfoState: QuestInfoState) {
    var isRandom by remember { mutableStateOf(questInfoState.rewardMin != questInfoState.rewardMax) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Random Reward")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isRandom,
                onCheckedChange = { isRandom = it }
            )
        }

        if (isRandom) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = questInfoState.rewardMin.toString(),
                    onValueChange = { value ->
                        questInfoState.rewardMin = value.toIntOrNull() ?: 0
                    },
                    label = { Text("Min Reward") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = questInfoState.rewardMax.toString(),
                    onValueChange = { value ->
                        questInfoState.rewardMax = value.toIntOrNull() ?: 0
                    },
                    label = { Text("Max Reward") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OutlinedTextField(
                value = questInfoState.rewardMin.toString(),
                onValueChange = { value ->
                    val amount = value.toIntOrNull() ?: 0
                    questInfoState.rewardMin = amount
                    questInfoState.rewardMax = amount
                },
                label = { Text("Reward") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
