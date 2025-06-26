package neth.iecal.questphone.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import neth.iecal.questphone.data.settings.SettingsRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings = repository.settings

    fun onQuestCreationChanged(isEnabled: Boolean) {
        repository.updateQuestCreation(isEnabled)
    }

    fun onQuestDeletionChanged(isEnabled: Boolean) {
        repository.updateQuestDeletion(isEnabled)
    }

    fun onItemCreationChanged(isEnabled: Boolean) {
        repository.updateItemCreation(isEnabled)
    }

    fun onItemDeletionChanged(isEnabled: Boolean) {
        repository.updateItemDeletion(isEnabled)
    }

    fun onSettingsLockChanged(isLocked: Boolean, password: String?, lockoutEndDate: Long?) {
        repository.updateSettingsLock(isLocked, password, lockoutEndDate)
    }
}

class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
