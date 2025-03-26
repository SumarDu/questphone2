package launcher.launcher.ui.screens.onboard

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import launcher.launcher.data.AppInfo
import launcher.launcher.utils.reloadApps
import androidx.compose.material3.TextButton
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog

@Composable
fun SelectApps(isNextEnabled: MutableState<Boolean>) {
    val context = LocalContext.current

    val isLoading = remember { mutableStateOf(true) }
    val appsState = remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val selectedApps = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            reloadApps(context.packageManager, context)
                .onSuccess { apps ->
                    appsState.value = apps
                    isLoading.value = false
                }
                .onFailure { error ->
                    Log.e("SelectApps", "Error loading apps: $error")
                    Toast.makeText(context, "Error loading apps: $error", Toast.LENGTH_SHORT).show()
                    isLoading.value = false
                }
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "Select Distractions",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "These might be social media or games. Leave blank and tap Next to skip blocking.",
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if(selectedApps.isNotEmpty()){
            AccessibilityPanel()
            isNextEnabled.value = false
        }else {
            isNextEnabled.value = true
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items(appsState.value) { (appName, packageName) ->
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
                            if (it) selectedApps.add(packageName) else selectedApps.remove(
                                packageName
                            )
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
}
@Composable
fun AccessibilityPanel() {
    var showDialog by remember { mutableStateOf(false) }

    // Panel
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This feature needs Accessibility Service",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121), // Dark gray for contrast
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Learn More",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Dialog
    if (showDialog) {
        AccessibilityDialog(onDismiss = { showDialog = false })
    }
}

@Composable
fun AccessibilityDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Why We Need Accessibility",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "This feature uses Accessibility Service to:\n" +
                            "• Block Selected Distracting Apps\n" +
                            "• Double Touch to turn off screen\n" +
                            "\n This is entirely optional. If you do not enable this, BlankPhone will operate as a normal Launcher cum Habit Tracker. " +
                            "All of your data remains completely secure on your device. We never share or store your data to our servers or third party. Our top " +
                            "priority is your data security.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    Text(
                        text = "Got It",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}