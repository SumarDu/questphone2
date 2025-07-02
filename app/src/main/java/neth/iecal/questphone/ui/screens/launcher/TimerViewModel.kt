package neth.iecal.questphone.ui.screens.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerRepository
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.getCurrentDate
import java.time.Duration
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val timerRepository = TimerRepository(application)
    private val questDao = QuestDatabaseProvider.getInstance(application).questDao()

    private val _timerText = MutableStateFlow("--:--")
    val timerText: StateFlow<String> = _timerText.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.INACTIVE)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _questFinishedEvent = MutableSharedFlow<String>()
    val questFinishedEvent = _questFinishedEvent.asSharedFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            timerRepository.timerStateFlow.first()
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                updateTimer()
                delay(1000)
            }
        }
    }

    private suspend fun updateTimer() {
        val allQuests = questDao.getAllQuests().first()
        val now = System.currentTimeMillis()
        val today = getCurrentDate()

        // 1. Check for active quest
        val activeQuest = allQuests.firstOrNull { it.quest_started_at > 0 && it.last_completed_on != today }

        if (activeQuest != null && activeQuest.quest_duration_minutes > 0) {
            val questStartTime = activeQuest.quest_started_at
            val questEndTime = questStartTime + TimeUnit.MINUTES.toMillis(activeQuest.quest_duration_minutes.toLong())

            if (now < questEndTime) {
                _timerMode.value = TimerMode.QUEST_COUNTDOWN
                val remaining = Duration.ofMillis(questEndTime - now)
                _timerText.value = formatDuration(remaining)
            } else {
                if (_timerMode.value == TimerMode.QUEST_COUNTDOWN) {
                    _questFinishedEvent.emit(activeQuest.id)
                }
                _timerMode.value = TimerMode.OVERTIME
                val overtime = Duration.ofMillis(now - questEndTime)
                _timerText.value = formatDuration(overtime)
            }
            return
        }

        // 2. Check for a recently completed quest for a break
        val recentlyCompleted = allQuests
            .filter { it.last_completed_on == today && it.break_duration_minutes > 0 && it.last_completed_at > 0 }
            .maxByOrNull { it.last_completed_at }

        if (recentlyCompleted != null) {
            val breakEndTime = recentlyCompleted.last_completed_at + TimeUnit.MINUTES.toMillis(recentlyCompleted.break_duration_minutes.toLong())
            if (now < breakEndTime) {
                _timerMode.value = TimerMode.BREAK
                val remaining = Duration.ofMillis(breakEndTime - now)
                _timerText.value = formatDuration(remaining)
                return
            }
        }

        // 3. Otherwise, inactive
        _timerMode.value = TimerMode.INACTIVE
        _timerText.value = "--:--"
    }

    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val absSeconds = kotlin.math.abs(seconds)
        val positive = String.format(
            "%02d:%02d",
            (absSeconds % 3600) / 60,
            absSeconds % 60
        )
        return if (seconds < 0) "-$positive" else positive
    }
}
