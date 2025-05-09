package launcher.launcher.ui.screens.quest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import launcher.launcher.data.quest.BasicQuestInfo
import launcher.launcher.ui.navigation.Screen
import launcher.launcher.utils.QuestHelper

@Composable
fun ListAllQuests(navHostController: NavHostController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Button(
                onClick = { navHostController.navigate(Screen.AddNewQuest.route) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(text = "Add Quest")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->

        val questHelper = QuestHelper(LocalContext.current)
        val questList = questHelper.getQuestList()
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
            ) {
            QuestList(navHostController,questList, itemTop = {
                SearchBar(
                    query = "",
                    onQueryChanged = {}
                )
            })
        }
    }

}

@Composable
fun QuestList(navHostController: NavController, questList: List<BasicQuestInfo>, itemTop: @Composable () -> Unit = {}, itemBottom: @Composable () -> Unit = {}) {

    LazyColumn {
        item {
            itemTop()
        }
        items(questList){questBase: BasicQuestInfo ->
            QuestItem(
                title = questBase.title,
                reward = questBase.reward,
                onClick = {
//                    val data = Json.encodeToString<BasicQuestInfo>(questBase)
////                    navHostController.navigate(Screen.ViewQuest.route + data)
//                    navHostController.navigate(Screen.QuestStats.route)
                    navHostController.navigate(Screen.QuestStats.route + Json.encodeToString<BasicQuestInfo>(questBase))
//
////                    )
                }
            )
        }
        item {
            itemBottom()
        }
    }
}

@Composable
private fun QuestItem(
    title: String,
    reward: Int,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Reward: $reward",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

}

@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        placeholder = { Text("Search...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
        singleLine = true
    )
}
