package neth.iecal.questphone.data.settings

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsData(
    val isQuestCreationEnabled: Boolean = true,
    val isQuestDeletionEnabled: Boolean = true,
    val isItemCreationEnabled: Boolean = true,
    val isItemDeletionEnabled: Boolean = true,
    val isSettingsLocked: Boolean = false,
    val settingsLockPassword: String? = null,
    val settingsLockoutEndDate: Long? = null
)

class SettingsRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _settings = MutableStateFlow(loadSettings())
    val settings = _settings.asStateFlow()

    private fun loadSettings(): SettingsData {
        val json = sharedPreferences.getString("settings_data", null)
        return if (json != null) {
            gson.fromJson(json, SettingsData::class.java)
        } else {
            SettingsData()
        }
    }

    fun saveSettings(settingsData: SettingsData) {
        val json = gson.toJson(settingsData)
        sharedPreferences.edit().putString("settings_data", json).apply()
        _settings.value = settingsData
    }

    fun updateQuestCreation(isEnabled: Boolean) {
        val newSettings = _settings.value.copy(isQuestCreationEnabled = isEnabled)
        saveSettings(newSettings)
    }

    fun updateQuestDeletion(isEnabled: Boolean) {
        val newSettings = _settings.value.copy(isQuestDeletionEnabled = isEnabled)
        saveSettings(newSettings)
    }

    fun updateItemCreation(isEnabled: Boolean) {
        val newSettings = _settings.value.copy(isItemCreationEnabled = isEnabled)
        saveSettings(newSettings)
    }

    fun updateItemDeletion(isEnabled: Boolean) {
        val newSettings = _settings.value.copy(isItemDeletionEnabled = isEnabled)
        saveSettings(newSettings)
    }

    fun updateSettingsLock(isLocked: Boolean, password: String?, lockoutEndDate: Long?) {
        val newSettings = _settings.value.copy(
            isSettingsLocked = isLocked,
            settingsLockPassword = password,
            settingsLockoutEndDate = lockoutEndDate
        )
        saveSettings(newSettings)
    }
}
