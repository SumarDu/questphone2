package launcher.launcher.ui.screens.quest.setup

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import launcher.launcher.data.IntegrationId
import launcher.launcher.ui.screens.tutorial.QuestTutorial
import launcher.launcher.utils.VibrationHelper

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetIntegration(navController: NavHostController) {

    Scaffold()
    { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)

        ) {
            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)

            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 32.dp),
                    text = "Select Integration",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                )

                IntegrationsList{
                    navController.navigate(
                        it.name
                    )
                }
            }
        }

    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntegrationsList(
    onSelected: (IntegrationId)-> Unit,
) {
    val currentDocLink = remember { mutableStateOf<String?>(null) }

    BackHandler(currentDocLink.value!=null) {
        currentDocLink.value = null
    }
    if(currentDocLink.value !=null){
        QuestTutorial(url = currentDocLink.value!!)
    }else{

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            items(IntegrationId.entries) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {onSelected(item) },
                            onLongClick = {
                                VibrationHelper.vibrate(100)
                                currentDocLink.value = item.docLink
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onSelected(item) }) {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.label,
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal
                            )
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

