package launcher.launcher.ui.screens.quest.setup.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.Constants
import launcher.launcher.R
import launcher.launcher.models.IntegrationInfo

@Composable
fun IntegrationsList(
    selectedItem: MutableState<Int?>
) {
    val items = listOf(
        IntegrationInfo(R.drawable.baseline_timer_24, "Focus", "Block all apps except the essential ones for a set period, allowing you to stay focused on your work.", Constants.INTEGRATION_ID_FOCUS),
        IntegrationInfo(R.drawable.baseline_phone_android_24, "App", "Restrict access to a single app while blocking everything else.", Constants.INTEGRATION_ID_APP_FOCUS),
        IntegrationInfo(R.drawable.baseline_directions_run_24, "Health Connect", "Earn coins by doing workout.", Constants.INTEGRATION_ID_HEALTH_CONNECT),
        IntegrationInfo(R.drawable.baseline_extension_24, "Add", "Add more integrations")
    )


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        items(items) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedItem.value = item.id },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedItem.value = item.id }) {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label,
                        modifier = Modifier.size(50.dp),
                        tint = if (selectedItem.value == item.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (selectedItem.value == item.id) FontWeight.Bold else FontWeight.Normal
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
