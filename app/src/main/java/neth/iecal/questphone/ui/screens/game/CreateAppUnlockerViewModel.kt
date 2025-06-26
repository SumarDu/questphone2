package neth.iecal.questphone.ui.screens.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.AppInfo
import neth.iecal.questphone.data.game.AppUnlockerItem
import neth.iecal.questphone.data.game.AppUnlockerItemDao

import neth.iecal.questphone.utils.reloadApps

class CreateAppUnlockerViewModel(private val appUnlockerItemDao: AppUnlockerItemDao) : ViewModel() {
    var apps by mutableStateOf<List<AppInfo>>(emptyList())
    var selectedApp by mutableStateOf<AppInfo?>(null)
    var price by mutableStateOf("")
    var hours by mutableStateOf("")
    var minutes by mutableStateOf("")

    fun loadApps(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sp = context.getSharedPreferences("distractions", Context.MODE_PRIVATE)
                val distractionApps = sp.getStringSet("distracting_apps", emptySet<String>()) ?: emptySet()

                reloadApps(context.packageManager, context)
                    .onSuccess { loadedApps ->
                        apps = loadedApps.filter { distractionApps.contains(it.packageName) }
                    }
                    .onFailure {
                        // Handle error
                    }
            }
        }
    }

    fun saveAppUnlocker(onSuccess: () -> Unit) {
        val app = selectedApp ?: return
        val priceValue = price.toIntOrNull() ?: return
        val hoursValue = hours.toIntOrNull() ?: 0
        val minutesValue = minutes.toIntOrNull() ?: 0
        val totalMinutes = (hoursValue * 60) + minutesValue

        if (totalMinutes <= 0) return // Or show an error

        val newItem = AppUnlockerItem(
            appName = app.name,
            packageName = app.packageName,
            price = priceValue,
            unlockDurationMinutes = totalMinutes
        )

        viewModelScope.launch {
            appUnlockerItemDao.insert(newItem)
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }
}

class CreateAppUnlockerViewModelFactory(private val appUnlockerItemDao: AppUnlockerItemDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateAppUnlockerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateAppUnlockerViewModel(appUnlockerItemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
