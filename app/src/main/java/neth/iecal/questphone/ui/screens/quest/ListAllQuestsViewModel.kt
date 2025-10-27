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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDao
import neth.iecal.questphone.data.settings.SettingsRepository
import neth.iecal.questphone.data.DayOfWeek
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.SchedulingInfo
import neth.iecal.questphone.data.SchedulingType
import neth.iecal.questphone.data.quest.CalendarQuest
import neth.iecal.questphone.services.CalendarSyncService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.saveUserInfo

enum class QuestTab {
    ALL, REPEATING, CLONED
}

class ListAllQuestsViewModel(application: Application, private val questDao: QuestDao) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(QuestTab.ALL)
    val selectedTab = _selectedTab.asStateFlow()

    val questToDelete = MutableStateFlow<CommonQuestInfo?>(null)

    val isEditingEnabled = settingsRepository.settings.map { it.isEditingEnabled }

    private val permanentQuests: Flow<List<CommonQuestInfo>> = questDao.getPermanentQuests()
    private val temporaryQuests: Flow<List<CommonQuestInfo>> = questDao.getClonedQuests()

    // Helper functions to categorize quests
    private fun isRepeatingQuest(quest: CommonQuestInfo): Boolean {
        return quest.selected_days.isNotEmpty()
    }
    

    
    private fun isClonedQuest(quest: CommonQuestInfo): Boolean {
        // A quest is cloned if it's marked as a clone.
        return (quest.title.contains("(Clone)") || quest.id.contains("clone"))
    }
    
    // Combined filtering based on tab and search query
    val filteredQuests = combine(permanentQuests, temporaryQuests, searchQuery, selectedTab) { permanent, temporary, query, tab ->
        val allQuestsList = permanent + temporary

        val questsToShow = when (tab) {
            QuestTab.ALL -> allQuestsList
            QuestTab.REPEATING -> allQuestsList.filter { isRepeatingQuest(it) }

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

    fun clearAllRewards() {
        viewModelScope.launch {
            if (isEditingEnabled.first()) {
                var changed = false
                if (User.userInfo.coins != 0) { User.userInfo.coins = 0; changed = true }
                if (User.userInfo.diamonds != 0) { User.userInfo.diamonds = 0; changed = true }
                if (User.userInfo.diamonds_pending != 0) { User.userInfo.diamonds_pending = 0; changed = true }
                if (changed) {
                    User.saveUserInfo()
                    Toast.makeText(getApplication(), "All rewards destroyed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), "No rewards to destroy", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(getApplication(), "Editing is disabled in settings", Toast.LENGTH_SHORT).show()
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
            if (isEditingEnabled.first()) {
                val originalQuest = questDao.getQuestById(quest.id)
                originalQuest?.let {
                    val clonedQuest = it.copy(id = UUID.randomUUID().toString(), title = "${it.title} (Clone)")
                    questDao.upsertQuest(clonedQuest)
                    Toast.makeText(getApplication(), "Quest cloned", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(getApplication(), "Editing is disabled in settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onQuestDeleteRequest(quest: CommonQuestInfo) {
        questToDelete.value = quest
    }

    fun onQuestDeleteConfirm() {
        viewModelScope.launch {
            if (isEditingEnabled.first()) {
                questToDelete.value?.let {
                    questDao.deleteQuest(it)
                }
            } else {
                Toast.makeText(getApplication(), "Editing is disabled in settings", Toast.LENGTH_SHORT).show()
            }
            questToDelete.value = null
        }
    }

    fun onQuestDeleteCancel() {
        questToDelete.value = null
    }

    fun onQuestResetRequest(quest: CommonQuestInfo) {
        viewModelScope.launch {
            if (isEditingEnabled.first()) {
                val updatedQuest = quest.copy(
                    last_completed_on = "0001-01-01",
                    quest_started_at = 0L,
                    last_completed_at = 0L
                )
                questDao.upsertQuest(updatedQuest)
                Toast.makeText(getApplication(), "Quest reset successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "Editing is disabled in settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun syncCalendar() {
        viewModelScope.launch {
            val calendarSyncService = CalendarSyncService(getApplication(), settingsRepository)
            val calendarQuests = calendarSyncService.getCalendarEvents()
            val dbQuests = questDao.getAllQuestsSuspend()
                .filter { it.calendar_event_id != null }
                .associateBy { it.calendar_event_id!! }

            val questsToUpsert = mutableListOf<CommonQuestInfo>()
            val questsToDelete = dbQuests.toMutableMap()

            for (calendarQuest in calendarQuests) {
                val existingQuest = dbQuests[calendarQuest.eventId]
                questsToDelete.remove(calendarQuest.eventId)

                val questFromCalendar = mapCalendarQuestToCommonQuestInfo(calendarQuest, existingQuest)

                if (existingQuest == null || hasQuestChanged(existingQuest, questFromCalendar)) {
                    questsToUpsert.add(questFromCalendar)
                }
            }

            if (questsToUpsert.isNotEmpty()) {
                questDao.upsertAll(questsToUpsert)
            }

            if (questsToDelete.isNotEmpty()) {
                questsToDelete.values.forEach { questDao.deleteQuest(it) }
            }

            Toast.makeText(getApplication(), "Calendar synced successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mapCalendarQuestToCommonQuestInfo(calendarQuest: CalendarQuest, existingQuest: CommonQuestInfo?): CommonQuestInfo {
        val (schedulingInfo, selectedDays, autoDestruct) = parseCalendarEventScheduling(calendarQuest)
        
        return CommonQuestInfo(
            id = existingQuest?.id ?: UUID.randomUUID().toString(),
            title = calendarQuest.title,
            instructions = calendarQuest.description,
            reward_min = calendarQuest.rewardMin,
            reward_max = calendarQuest.rewardMax,
            quest_duration_minutes = calendarQuest.duration,
            break_duration_minutes = calendarQuest.breakMinutes,
            ai_photo_proof = calendarQuest.aiPhotoProofPrompt != null,
            ai_photo_proof_description = calendarQuest.aiPhotoProofPrompt ?: "",
            integration_id = IntegrationId.SWIFT_MARK,
            calendar_event_id = calendarQuest.eventId,
            scheduling_info = schedulingInfo,
            selected_days = selectedDays,
            auto_destruct = autoDestruct,
            last_updated = System.currentTimeMillis()
        )
    }
    
    private fun parseCalendarEventScheduling(calendarQuest: CalendarQuest): Triple<SchedulingInfo, Set<DayOfWeek>, String> {
        val rrule = calendarQuest.rrule
        
        // Check for weekly recurring event
        if (!rrule.isNullOrEmpty() && rrule.contains("FREQ=WEEKLY")) {
            val dayOfWeek = getDayOfWeekFromTimestamp(calendarQuest.startTime)
            val selectedDays = setOf(dayOfWeek)

            val schedulingInfo = SchedulingInfo(
                type = SchedulingType.WEEKLY,
                selectedDays = selectedDays
            )

            val autoDestruct = calendarQuest.until?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
            } ?: "9999-12-31"

            return Triple(schedulingInfo, selectedDays, autoDestruct)

        // Check for monthly recurring event
        } else if (!rrule.isNullOrEmpty() && rrule.contains("FREQ=MONTHLY")) {
            val intervalRegex = "INTERVAL=([0-9]+)".toRegex()
            val intervalMatch = intervalRegex.find(rrule)
            val interval = intervalMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

            if (interval > 1) {
                // Treat as a one-time quest if interval is > 1 month
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val specificDate = dateFormat.format(Date(calendarQuest.startTime))
                val schedulingInfo = SchedulingInfo(
                    type = SchedulingType.SPECIFIC_DATE,
                    specificDate = specificDate
                )
                return Triple(schedulingInfo, emptySet(), specificDate)
            }

            val byDayRegex = "BYDAY=([0-9A-Z-]+)".toRegex()
            val byDayMatch = byDayRegex.find(rrule)

            if (byDayMatch != null) {
                val byDayValue = byDayMatch.groupValues[1]
                val weekInMonth = byDayValue.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: 0
                val dayOfWeekStr = byDayValue.filter { it.isLetter() }
                val dayOfWeek = mapRruleDayToAppDay(dayOfWeekStr)


                val schedulingInfo = SchedulingInfo(
                    type = SchedulingType.MONTHLY_BY_DAY,
                    monthlyDayOfWeek = dayOfWeek,
                    monthlyWeekInMonth = weekInMonth
                )

                val autoDestruct = calendarQuest.until?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                } ?: "9999-12-31"

                return Triple(schedulingInfo, emptySet(), autoDestruct)
            } else {
                val dayOfMonth = getDayOfMonthFromTimestamp(calendarQuest.startTime)

                val schedulingInfo = SchedulingInfo(
                    type = SchedulingType.MONTHLY_DATE,
                    monthlyDate = dayOfMonth
                )

                val autoDestruct = calendarQuest.until?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                } ?: "9999-12-31"

                return Triple(schedulingInfo, emptySet(), autoDestruct)
            }
        // Handle single occurrence event
        } else {
            val schedulingInfo = SchedulingInfo(
                type = SchedulingType.SPECIFIC_DATE,
                specificDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(calendarQuest.startTime))
            )

            val autoDestruct = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(calendarQuest.endTime))

            return Triple(schedulingInfo, emptySet(), autoDestruct)
        }
    }
    
    private fun mapRruleDayToAppDay(rruleDay: String): DayOfWeek {
        return when (rruleDay.uppercase()) {
            "SU" -> DayOfWeek.SUN
            "MO" -> DayOfWeek.MON
            "TU" -> DayOfWeek.TUE
            "WE" -> DayOfWeek.WED
            "TH" -> DayOfWeek.THU
            "FR" -> DayOfWeek.FRI
            "SA" -> DayOfWeek.SAT
            else -> throw IllegalArgumentException("Unknown RRULE day: $rruleDay")
        }
    }

    private fun getDayOfWeekFromTimestamp(timestamp: Long): DayOfWeek {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        return when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> DayOfWeek.SUN
            java.util.Calendar.MONDAY -> DayOfWeek.MON
            java.util.Calendar.TUESDAY -> DayOfWeek.TUE
            java.util.Calendar.WEDNESDAY -> DayOfWeek.WED
            java.util.Calendar.THURSDAY -> DayOfWeek.THU
            java.util.Calendar.FRIDAY -> DayOfWeek.FRI
            java.util.Calendar.SATURDAY -> DayOfWeek.SAT
            else -> DayOfWeek.MON // fallback
        }
    }

    private fun getDayOfMonthFromTimestamp(timestamp: Long): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(java.util.Calendar.DAY_OF_MONTH)
    }

    private fun hasQuestChanged(oldQuest: CommonQuestInfo, newQuest: CommonQuestInfo): Boolean {
        return oldQuest.title != newQuest.title ||
                oldQuest.instructions != newQuest.instructions ||
                oldQuest.reward_min != newQuest.reward_min ||
                oldQuest.reward_max != newQuest.reward_max ||
                oldQuest.quest_duration_minutes != newQuest.quest_duration_minutes ||
                oldQuest.break_duration_minutes != newQuest.break_duration_minutes ||
                oldQuest.ai_photo_proof != newQuest.ai_photo_proof ||
                oldQuest.ai_photo_proof_description != newQuest.ai_photo_proof_description
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
