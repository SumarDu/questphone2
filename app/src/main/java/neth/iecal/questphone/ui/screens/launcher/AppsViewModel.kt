package neth.iecal.questphone.ui.screens.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.os.UserManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.local.AppAlias
import neth.iecal.questphone.data.local.AppAliasDao

data class AppInfo(val originalLabel: String, val label: String, val packageName: String, val applicationInfo: ApplicationInfo)

class AppsViewModel(private val context: Context, private val appAliasDao: AppAliasDao) : ViewModel() {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps = _apps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredApps = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            flow {
                val filtered = if (query.isBlank()) {
                    _apps.value
                } else {
                    _apps.value.filter {
                        it.label.contains(query, true) || it.packageName.contains(query, true)
                    }
                }
                emit(filtered)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            loadApps()
        }
    }

    private suspend fun loadApps() {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        val systemAppsFlow = withContext(Dispatchers.IO) {
            val profiles = userManager.userProfiles
            val apps = profiles.flatMap { userHandle ->
                launcherApps.getActivityList(null, userHandle).mapNotNull { activityInfo ->
                    try {
                        val applicationInfo = activityInfo.applicationInfo
                        val label = activityInfo.label.toString()
                        AppInfo(label, label, applicationInfo.packageName, applicationInfo)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .distinctBy { it.packageName }
            MutableStateFlow(apps)
        }

        appAliasDao.getAllAliases().combine(systemAppsFlow) { aliases, systemApps ->
            val aliasMap = aliases.associateBy { it.packageName }
            val combinedApps = systemApps.map {
                it.copy(label = aliasMap[it.packageName]?.alias ?: it.originalLabel)
            }.sortedBy { it.label.lowercase() }
            _apps.value = combinedApps
            // Initial filtering is now handled by the flow
        }.collect {}
    }

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun saveAlias(packageName: String, alias: String) {
        viewModelScope.launch {
            appAliasDao.insertAlias(AppAlias(packageName, alias))
        }
    }


}
