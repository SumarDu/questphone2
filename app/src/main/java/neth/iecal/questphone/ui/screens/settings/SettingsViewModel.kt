package neth.iecal.questphone.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.ai.GeminiPro
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.data.remote.SupabaseSyncService

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val supabaseSyncService = SupabaseSyncService(application)

    val settings = repository.settings

    fun getSettingsRepository(): SettingsRepository {
        return repository
    }

    private val _geminiResponse = MutableStateFlow<String?>(null)
    val geminiResponse = _geminiResponse.asStateFlow()

    fun onQuestCreationChanged(isEnabled: Boolean) {
        viewModelScope.launch {
        repository.updateQuestCreation(isEnabled)
        }
    }

    fun onQuestDeletionChanged(isEnabled: Boolean) {
        viewModelScope.launch {
        repository.updateQuestDeletion(isEnabled)
        }
    }

    fun onItemCreationChanged(isEnabled: Boolean) {
        viewModelScope.launch {
        repository.updateItemCreation(isEnabled)
        }
    }

    fun onItemDeletionChanged(isEnabled: Boolean) {
        viewModelScope.launch {
        repository.updateItemDeletion(isEnabled)
        }
    }



    fun onSettingsLockChanged(isLocked: Boolean, password: String?, lockoutEndDate: Long?) {
        viewModelScope.launch {
        repository.updateSettingsLock(isLocked, password, lockoutEndDate)
        }
    }

    fun updateAutoSyncTime(minutes: Int?) {
        viewModelScope.launch {
            repository.updateAutoSyncTime(minutes)
        }
    }

    fun updateSelectedCalendars(calendarIds: Set<String>) {
        viewModelScope.launch {
            repository.updateSelectedCalendars(calendarIds)
        }
    }

    fun generateTextWithGemini(prompt: String) {
        val apiKey = settings.value.geminiApiKey ?: ""
        viewModelScope.launch {
            _geminiResponse.value = GeminiPro.generate(prompt, apiKey)
        }
    }

    fun saveGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            repository.updateGeminiApiKey(apiKey)
        }
    }

    fun clearGeminiResponse() {
        _geminiResponse.value = null
    }

    fun updateShowRepeatingQuestsInDialog(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowRepeatingQuestsInDialog(show)
        }
    }

    fun updateShowClonedQuestsInDialog(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowClonedQuestsInDialog(show)
        }
    }

    fun updateShowOneTimeQuestsInDialog(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowOneTimeQuestsInDialog(show)
        }
    }

    fun createCheckpoint(checkpointName: String, comments: String? = null) {
        viewModelScope.launch {
            supabaseSyncService.createCheckpoint(checkpointName, comments)
        }
    }

    fun onEditingPermissionChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            // Get current state to check if we're turning off editing permission
            val currentSettings = repository.settings.value
            val wasEditingEnabled = currentSettings.isEditingEnabled

            // Update the editing permission
            repository.updateEditingPermission(isEnabled)

            // Create checkpoint when turning ON editing permission
            if (!wasEditingEnabled && isEnabled) {
                supabaseSyncService.createCheckpoint("editing permission enabled")
            }
        }
    }

    fun updateUnplannedBreakReasons(reasons: List<String>) {
        viewModelScope.launch {
            repository.updateUnplannedBreakReasons(reasons)
        }
    }

    // Overdue penalty settings
    fun setOverduePenaltyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateOverduePenaltyEnabled(enabled)
        }
    }

    fun setOverduePenaltyWindow(minutes: Int) {
        viewModelScope.launch {
            repository.updateOverduePenaltyWindow(minutes)
        }
    }

    fun setOverduePenaltyCoins(coins: Int) {
        viewModelScope.launch {
            repository.updateOverduePenaltyCoins(coins)
        }
    }

    // Diamond exchange settings
    fun setDiamondExchangeDiamonds(diamonds: Int) {
        viewModelScope.launch {
            repository.updateDiamondExchangeDiamonds(diamonds)
        }
    }

    fun setDiamondExchangeCoins(coins: Int) {
        viewModelScope.launch {
            repository.updateDiamondExchangeCoins(coins)
        }
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
