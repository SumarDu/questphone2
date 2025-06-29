package neth.iecal.questphone.ui.screens.quest

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDao
import neth.iecal.questphone.data.settings.SettingsRepository

class ListAllQuestsViewModel(application: Application, private val questDao: QuestDao) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _quests = MutableStateFlow<List<CommonQuestInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val questToDelete = MutableStateFlow<CommonQuestInfo?>(null)

    val filteredQuests = _quests.combine(_searchQuery) { quests, query ->
        if (query.isBlank()) {
            quests
        } else {
            quests.filter {
                it.title.contains(query, ignoreCase = true) || it.instructions.contains(query, ignoreCase = true)
            }
        }
    }

    init {
        loadQuests()
    }

    private fun loadQuests() {
        viewModelScope.launch {
            questDao.getAllQuests().collect {
                _quests.value = it
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onQuestDeleteRequest(quest: CommonQuestInfo) {
        questToDelete.value = quest
    }

    fun onQuestDeleteConfirm() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.isQuestDeletionEnabled) {
                questToDelete.value?.let {
                    questDao.deleteQuest(it)
                }
            } else {
                Toast.makeText(getApplication(), "Quest deletion is disabled in settings", Toast.LENGTH_SHORT).show()
            }
            questToDelete.value = null
        }
    }

    fun onQuestDeleteCancel() {
        questToDelete.value = null
    }
}

class ListAllQuestsViewModelFactory(private val application: Application, private val questDao: QuestDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListAllQuestsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListAllQuestsViewModel(application, questDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
