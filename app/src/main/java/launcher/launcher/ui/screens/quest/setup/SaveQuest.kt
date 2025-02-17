package launcher.launcher.ui.screens.quest.setup


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import launcher.launcher.Constants
import launcher.launcher.models.DayOfWeek
import launcher.launcher.ui.navigation.AddNewQuestSubScreens

@Composable
fun ReviewFinalSettings(
    reward: MutableIntState,
    selectedDays: MutableState<Set<DayOfWeek>>,
    selectedIntegration: MutableState<Int?>,
    selectedFocusApp: MutableState<String>,
    selectedUnrestrictedApps: MutableState<Set<String>>
) {
    Text("Review Settings",
        style = MaterialTheme.typography.headlineLarge)

    Text("Please cross check all you choices. You won't be allowed to change any configurations for this quest until you pay 150 coins.",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp))

    val pm = LocalContext.current.packageManager
    LazyColumn(
        modifier = Modifier.padding(top = 32.dp)
    ) {
        item {
            ReviewItem("Selected Days",selectedDays.value.toString())
        }
        item {
            ReviewItem("Reward", reward.intValue.toString())
        }
        item {
            ReviewItem("Integration", selectedIntegration.value.toString())
        }

        when(selectedIntegration.value){

            // Review Settings for App Focus Integration
            Constants.INTEGRATION_ID_APP_FOCUS -> item {
                ReviewItem("Selected App",
                pm.getApplicationLabel(pm.getApplicationInfo(selectedFocusApp.value,0)).toString()
                )
            }


            // Review Settings for Focus Integration
            Constants.INTEGRATION_ID_FOCUS -> {
                item {
                    var selectedApps = ""
                    selectedUnrestrictedApps.value.forEach { value ->
                        selectedApps +=  pm.getApplicationLabel(pm.getApplicationInfo(value,0)).toString()
                        selectedApps += " "
                    }
                    ReviewItem("Selected Unrestricted Apps", selectedApps)
                }

            }

        }
    }

}

@Composable
fun ReviewItem(title:String, value:String){
    Column(
        Modifier.padding(vertical = 8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.size(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}