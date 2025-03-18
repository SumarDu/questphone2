package launcher.launcher.ui.screens.onboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import launcher.launcher.utils.getCachedApps

@Composable
fun SelectApps() {
    val context = LocalContext.current
    val apps = remember { getCachedApps(context).map { it.name to it.packageName } }
    val selectedApps = remember { mutableStateListOf<String>() }


    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        items(apps) { (appName, packageName) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selectedApps.contains(packageName)) {
                            selectedApps.remove(packageName)
                        } else {
                            selectedApps.add(packageName)
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedApps.contains(packageName),
                    onCheckedChange = {
                        if (it) selectedApps.add(packageName) else selectedApps.remove(packageName)
                    }
                )
                Text(
                    text = appName,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}