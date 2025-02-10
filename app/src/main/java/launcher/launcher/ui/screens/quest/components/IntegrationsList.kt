package launcher.launcher.ui.screens.quest.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import launcher.launcher.R

@Composable
fun IntegrationsList(
    selectedItem : MutableState<String?>
) {
    val items = listOf(
        Triple(R.drawable.baseline_timer_24, "Focus", "Block all apps except the essential ones for a set period, allowing you to stay focused on your work."),
        Triple(R.drawable.baseline_phone_android_24, "App", "Restrict access to a single app while blocking everything else."),
        Triple(R.drawable.baseline_directions_run_24, "Health Connect", "Earn coins by doing workout."),
        Triple(R.drawable.baseline_extension_24, "Add", "Add more integrations")
    )


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { (icon, label, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedItem.value = label }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedItem.value = label }) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = label,
                        modifier = Modifier.size(50.dp),
                        tint = if (selectedItem.value == label) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (selectedItem.value == label) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
