package neth.iecal.questphone.ui.screens.launcher

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.quest.QuestDao
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import neth.iecal.questphone.data.timer.TimerService.Companion.ACTION_ADD_TIME
import neth.iecal.questphone.data.timer.TimerService.Companion.ACTION_COMPLETE_QUEST
import neth.iecal.questphone.data.timer.TimerService.Companion.ACTION_END_BREAK_EARLY
import neth.iecal.questphone.data.timer.TimerService.Companion.ACTION_SUBMIT_UNPLANNED_BREAK_REASON
import neth.iecal.questphone.data.timer.TimerService.Companion.ACTION_STOP_UNPLANNED_BREAK
import neth.iecal.questphone.data.timer.TimerService.Companion.EXTRA_UNPLANNED_BREAK_REASON
import neth.iecal.questphone.data.timer.TimerService.Companion.LATER_MARKER
import neth.iecal.questphone.data.timer.TimerService.Companion.EXTRA_TIME_TO_ADD
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.timer.TimerState
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.UUID
import neth.iecal.questphone.utils.getCurrentDate

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val questDao = QuestDatabaseProvider.getInstance(application).questDao()
    val allQuests = questDao.getPermanentQuests()

    private val _timerText = MutableStateFlow("--:--")
    val timerText: StateFlow<String> = _timerText.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.INACTIVE)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _questFinishedEvent = MutableSharedFlow<String>()
    val questFinishedEvent = _questFinishedEvent.asSharedFlow()

    private var previousMode: TimerMode = TimerMode.INACTIVE
    private var previousInfoMode: TimerMode? = null

    init {
        viewModelScope.launch {
            TimerService.timerState.collect { state ->
                val currentMode = state.mode
                if (currentMode == TimerMode.INFO) {
                    if (previousInfoMode == null) {
                        previousInfoMode = previousMode
                    }
                } else {
                    if (previousInfoMode != null) {
                        previousInfoMode = null
                    }
                }

                _timerMode.value = state.mode
                _timerState.value = state
                _timerText.value = formatDuration(state.time)

                if (currentMode == TimerMode.OVERTIME && previousMode == TimerMode.QUEST_COUNTDOWN && state.activeQuestId != null) {
                    viewModelScope.launch {
                        _questFinishedEvent.emit(state.activeQuestId!!)
                    }
                }
                previousMode = currentMode
            }
        }
    }

    fun toggleInfoMode() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_TOGGLE_INFO_MODE
        }
        getApplication<Application>().startService(intent)
    }

    fun stopUnplannedBreak() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_UNPLANNED_BREAK
        }
        getApplication<Application>().startService(intent)
    }

    fun addTime(minutes: Int) {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, TimerService::class.java).apply {
            action = ACTION_ADD_TIME
            putExtra(EXTRA_TIME_TO_ADD, TimeUnit.MINUTES.toMillis(minutes.toLong()))
        }
        context.startService(intent)
    }

    fun completeQuest() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = ACTION_COMPLETE_QUEST
        }
        getApplication<Application>().startService(intent)
    }

    fun cloneAndStartQuest(quest: CommonQuestInfo) {
        viewModelScope.launch {
            val today = getCurrentDate()
            val clonedQuestsCount = questDao.getClonedQuestsCountForToday(today, "${quest.title} [C%")
            val newTitle = "${quest.title} [C${clonedQuestsCount + 1}]"

            val clonedQuest = quest.copy(
                id = UUID.randomUUID().toString(),
                title = newTitle,
                auto_destruct = today, // Will be deleted next day
                quest_started_at = System.currentTimeMillis(),
                last_completed_at = 0,
                last_completed_on = "",
                deadline_minutes = -1
            )
            questDao.upsertQuest(clonedQuest)
        }
    }

    fun startUnplannedBreak(reason: String) {
        Intent(getApplication(), TimerService::class.java).also {
            it.action = TimerService.ACTION_START_UNPLANNED_BREAK
            it.putExtra("UNPLANNED_BREAK_REASON", reason)
            getApplication<Application>().startService(it)
        }
    }

    fun startUnplannedBreakLater() {
        Intent(getApplication(), TimerService::class.java).also {
            it.action = TimerService.ACTION_START_UNPLANNED_BREAK
            it.putExtra("UNPLANNED_BREAK_REASON", LATER_MARKER)
            getApplication<Application>().startService(it)
        }
    }

    fun submitUnplannedBreakReason(reason: String) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = ACTION_SUBMIT_UNPLANNED_BREAK_REASON
            putExtra(EXTRA_UNPLANNED_BREAK_REASON, reason)
        }
        getApplication<Application>().startService(intent)
    }

    fun endBreakEarly() {
        Log.d("TimerViewModel", "endBreakEarly called. Sending intent to TimerService.")
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = ACTION_END_BREAK_EARLY
        }
        getApplication<Application>().startService(intent)
    }

    fun startUnlockTimer(durationMinutes: Int, packageName: String) {
        Log.d("TimerViewModel", "startUnlockTimer called. Sending intent to TimerService.")
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_START_UNLOCK_TIMER
            putExtra("unlock_duration_minutes", durationMinutes)
            putExtra("package_name", packageName)
        }
        getApplication<Application>().startService(intent)
    }

    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val absSeconds = kotlin.math.abs(seconds)

        val positive = if (absSeconds >= 3600) {
            String.format(
                "%02d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60
            )
        } else {
            String.format(
                "%02d:%02d",
                (absSeconds % 3600) / 60,
                absSeconds % 60
            )
        }

        return if (seconds < 0) "-$positive" else positive
    }
}
