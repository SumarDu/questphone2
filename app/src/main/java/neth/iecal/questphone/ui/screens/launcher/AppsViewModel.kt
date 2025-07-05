package neth.iecal.questphone.ui.screens.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.database.Cursor
import android.net.Uri
import android.os.UserHandle
import android.provider.ContactsContract
import android.os.UserManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.local.AppAlias
import neth.iecal.questphone.data.local.AppAliasDao

sealed class ListItem {
    data class App(val appInfo: AppInfo) : ListItem()
    data class Contact(val contactInfo: ContactInfo) : ListItem()
}

data class AppInfo(val originalLabel: String, val label: String, val packageName: String, val applicationInfo: ApplicationInfo, val user: UserHandle)
data class ContactInfo(val id: Long, val name: String, val phoneNumber: String)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AppsViewModel(private val context: Context, private val appAliasDao: AppAliasDao) : ViewModel() {
    private val _listItems = MutableStateFlow<List<ListItem>>(emptyList())
    val listItems = _listItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredListItems = combine(listItems, _searchQuery.debounce(300)) { items, query ->
        if (query.isBlank()) {
            items
        } else {
            items.filter {
                when (it) {
                    is ListItem.App -> it.appInfo.label.startsWith(query, true) || it.appInfo.packageName.startsWith(query, true)
                    is ListItem.Contact -> it.contactInfo.name.startsWith(query, true) || it.contactInfo.phoneNumber.startsWith(query, true)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    init {
        viewModelScope.launch {
            combine(loadApps(), loadContacts()) { apps: List<AppInfo>, contacts: List<ContactInfo> ->
                val appItems = apps.map { ListItem.App(it) }
                val contactItems = contacts.map { ListItem.Contact(it) }
                (appItems + contactItems).sortedBy {
                    when (it) {
                        is ListItem.App -> it.appInfo.label.lowercase()
                        is ListItem.Contact -> it.contactInfo.name.lowercase()
                    }
                }
            }.collect { combinedList ->
                _listItems.value = combinedList
            }
        }
    }

    private fun loadContacts(): Flow<List<ContactInfo>> = flow {
        try {
            val contacts = mutableListOf<ContactInfo>()
            val contentResolver = context.contentResolver
            val cursor: Cursor? = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)

            cursor?.use {
                val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val number = it.getString(numberColumn)
                    contacts.add(ContactInfo(id, name, number))
                }
            }
            emit(contacts)
        } catch (e: SecurityException) {
            // To prevent crash if READ_CONTACTS permission is not granted
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    private fun loadApps(): Flow<List<AppInfo>> {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        val systemAppsFlow: Flow<List<AppInfo>> = flow {
            val profiles = userManager.userProfiles
            val apps = profiles.flatMap { userHandle ->
                launcherApps.getActivityList(null, userHandle).mapNotNull { activityInfo ->
                    try {
                        val applicationInfo = activityInfo.applicationInfo
                        val packageName = applicationInfo.packageName
                        val label = activityInfo.label?.toString()
                        if (packageName != null && label != null) {
                            AppInfo(label, label, packageName, applicationInfo, activityInfo.user)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .distinctBy { it.packageName to it.user }
            emit(apps)
        }.flowOn(Dispatchers.IO)
        return appAliasDao.getAllAliases().combine(systemAppsFlow) { aliases: List<AppAlias>, systemApps: List<AppInfo> ->
            val aliasMap = aliases.associateBy { it.packageName }
            systemApps.map { appInfo ->
                appInfo.copy(label = aliasMap[appInfo.packageName]?.alias ?: appInfo.originalLabel)
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun saveAlias(packageName: String, alias: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appAliasDao.insertAlias(AppAlias(packageName, alias))
        }
    }


}
