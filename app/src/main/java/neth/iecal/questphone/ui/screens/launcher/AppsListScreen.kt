package neth.iecal.questphone.ui.screens.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.content.pm.PackageManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import neth.iecal.questphone.data.local.AppDatabase
import neth.iecal.questphone.ui.screens.launcher.ContactInfo
import neth.iecal.questphone.ui.screens.launcher.ListItem

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppsListScreen(
    navController: NavController,
    appsViewModel: AppsViewModel = viewModel(
        factory = AppsViewModelFactory(
            LocalContext.current,
            AppDatabase.getDatabase(LocalContext.current).appAliasDao()
        )
    )
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val launcherApps = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val focusRequester = remember { FocusRequester() }

    val filteredListItems by appsViewModel.filteredListItems.collectAsStateWithLifecycle()
    val query by appsViewModel.searchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(filteredListItems) {
        if (filteredListItems.size == 1 && query.isNotBlank()) {
            when (val item = filteredListItems.first()) {
                is ListItem.App -> {
                    val appToLaunch = item.appInfo
                    val launchIntent = pm.getLaunchIntentForPackage(appToLaunch.packageName)
                    if (launchIntent?.component != null) {
                        launcherApps.startMainActivity(launchIntent.component, appToLaunch.user, null, null)
                        appsViewModel.onQueryChanged("")
                    }
                }
                is ListItem.Contact -> {
                    // Do nothing, don't auto-call
                }
            }
        }
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("All Apps") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { appsViewModel.onQueryChanged(it) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .focusRequester(focusRequester)
                    .onKeyEvent {
                        if (it.key == Key.Enter) {
                            if (filteredListItems.isNotEmpty()) {
                                when (val item = filteredListItems.first()) {
                                    is ListItem.App -> {
                                        val appToLaunch = item.appInfo
                                        val launchIntent = pm.getLaunchIntentForPackage(appToLaunch.packageName)
                                        if (launchIntent?.component != null) {
                                            launcherApps.startMainActivity(launchIntent.component, appToLaunch.user, null, null)
                                            appsViewModel.onQueryChanged("")
                                        }
                                    }
                                    is ListItem.Contact -> {
                                        val contactToCall = item.contactInfo
                                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contactToCall.phoneNumber}"))
                                        ctx.startActivity(intent)
                                        appsViewModel.onQueryChanged("")
                                    }
                                }
                            }
                            true
                        } else {
                            false
                        }
                    },
                placeholder = { Text("Search apps") },
                singleLine = true
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(filteredListItems, key = { item ->
                    when (item) {
                        is ListItem.App -> "app-${item.appInfo.packageName}-${item.appInfo.user.hashCode()}"
                        is ListItem.Contact -> "contact-${item.contactInfo.id}"
                    }
                }) { item ->
                    when (item) {
                        is ListItem.App -> {
                            AppListItem(app = item.appInfo, onClick = { appInfo ->
                                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                                if (launchIntent?.component != null) {
                                    try {
                                        launcherApps.startMainActivity(launchIntent.component, appInfo.user, null, null)
                                    } catch (e: Exception) {
                                        // Handle exceptions, e.g., app not found for user
                                    }
                                }
                            }, onLongClick = {
                                selectedApp = item.appInfo
                                showRenameDialog = true
                            })
                        }
                        is ListItem.Contact -> {
                            ContactListItem(contact = item.contactInfo, onClick = { contactInfo ->
                                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contactInfo.phoneNumber}"))
                                ctx.startActivity(intent)
                            })
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            app = selectedApp!!,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                appsViewModel.saveAlias(selectedApp!!.packageName, newName)
                showRenameDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(app: AppInfo, onClick: (AppInfo) -> Unit, onLongClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick(app) },
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        AsyncImage(
            model = app.applicationInfo,
            contentDescription = app.label,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ContactListItem(contact: ContactInfo, onClick: (ContactInfo) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(contact) }
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Contact",
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RenameDialog(app: AppInfo, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(app.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename App") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("New name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

class AppsViewModelFactory(
    private val context: android.content.Context,
    private val appAliasDao: neth.iecal.questphone.data.local.AppAliasDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppsViewModel(context, appAliasDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 