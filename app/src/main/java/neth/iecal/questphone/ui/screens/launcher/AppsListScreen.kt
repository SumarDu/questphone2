package neth.iecal.questphone.ui.screens.launcher

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.ui.navigation.Screen
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(val label: String, val packageName: String)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppsListScreen(navController: NavController) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(Unit) {
        scope.launch {
            val entries = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                    .map { ri ->
                        val label = ri.loadLabel(pm).toString()
                        AppEntry(label, ri.activityInfo.packageName)
                    }
                    .sortedBy { it.label.lowercase() }
            }
            apps = entries
        }
    }

    val filtered = remember(apps, query) {
        if (query.text.isBlank()) apps else apps.filter {
            it.label.contains(query.text, true) || it.packageName.contains(query.text, true)
        }
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("All Apps") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Search apps") },
                singleLine = true
            )
            LazyVerticalGrid(columns = GridCells.Adaptive(90.dp), contentPadding = PaddingValues(8.dp)) {
                items(filtered, key = { entry -> entry.packageName }) { app ->
                    AppGridItem(app) { pkg ->
                        val launchIntent = pm.getLaunchIntentForPackage(pkg)
                        ctx.startActivity(launchIntent)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppGridItem(app: AppEntry, onClick: (String) -> Unit) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val icon = remember(app.packageName) {
        pm.getApplicationIcon(app.packageName)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick(app.packageName) }
    ) {
        Image(bitmap = icon.toBitmap().asImageBitmap(), contentDescription = app.label, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(4.dp))
        Text(app.label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
} 