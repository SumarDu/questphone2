package neth.iecal.questphone.ui.screens.quest

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDao
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.data.DayOfWeek

enum class QuestTab {
    ALL, REPEATING, CALENDAR, CLONED
}

class ListAllQuestsViewModel(application: Application, private val questDao: QuestDao) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(QuestTab.ALL)
    val selectedTab = _selectedTab.asStateFlow()

    val questToDelete = MutableStateFlow<CommonQuestInfo?>(null)

    private val permanentQuests: Flow<List<CommonQuestInfo>> = questDao.getPermanentQuests()
    private val temporaryQuests: Flow<List<CommonQuestInfo>> = questDao.getClonedQuests() // Note: This includes calendar quests

    // Helper functions to categorize quests
    private fun isRepeatingQuest(quest: CommonQuestInfo): Boolean {
        return quest.selected_days.isNotEmpty() && quest.calendar_event_id == null
    }
    
    private fun isCalendarQuest(quest: CommonQuestInfo): Boolean {
        return quest.calendar_event_id != null
    }
    
    private fun isClonedQuest(quest: CommonQuestInfo): Boolean {
        // A quest is cloned if it's marked as a clone and is not a calendar quest.
        return (quest.title.contains("(Clone)") || quest.id.contains("clone")) && !isCalendarQuest(quest)
    }
    
    // Combined filtering based on tab and search query
    val filteredQuests = combine(permanentQuests, temporaryQuests, searchQuery, selectedTab) { permanent, temporary, query, tab ->
        val allQuestsList = permanent + temporary

        val questsToShow = when (tab) {
            QuestTab.ALL -> allQuestsList
            QuestTab.REPEATING -> allQuestsList.filter { isRepeatingQuest(it) }
            QuestTab.CALENDAR -> allQuestsList.filter { isCalendarQuest(it) }
            QuestTab.CLONED -> allQuestsList.filter { isClonedQuest(it) }
        }
        
        if (query.isBlank()) {
            questsToShow
        } else {
            questsToShow.filter {
                it.title.contains(query, ignoreCase = true) || it.instructions.contains(query, ignoreCase = true)
            }
        }
    }
    
    // Separate flows for backward compatibility
    val filteredClonedQuests = temporaryQuests.combine(searchQuery) { quests, query ->
        val cloned = quests.filter { isClonedQuest(it) }
        if (query.isBlank()) {
            cloned
        } else {
            cloned.filter {
                it.title.contains(query, ignoreCase = true) || it.instructions.contains(query, ignoreCase = true)
            }
        }
    }

        fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun onTabSelected(tab: QuestTab) {
        _selectedTab.value = tab
    }

        fun onQuestCloneRequest(quest: CommonQuestInfo) {
        viewModelScope.launch {
            val originalQuest = questDao.getQuestById(quest.id)
            originalQuest?.let {
                                val clonedQuest = it.copy(id = UUID.randomUUID().toString(), title = "${it.title} (Clone)")
                questDao.upsertQuest(clonedQuest)
                Toast.makeText(getApplication(), "Quest cloned", Toast.LENGTH_SHORT).show()
            }
        }
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
