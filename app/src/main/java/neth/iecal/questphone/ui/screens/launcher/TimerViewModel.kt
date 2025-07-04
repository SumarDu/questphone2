package neth.iecal.questphone.ui.screens.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import neth.iecal.questphone.data.timer.TimerState
import java.time.Duration

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val _timerText = MutableStateFlow("--:--")
    val timerText: StateFlow<String> = _timerText.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.INACTIVE)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _questFinishedEvent = MutableSharedFlow<String>()
    val questFinishedEvent = _questFinishedEvent.asSharedFlow()

    private var previousMode: TimerMode = TimerMode.INACTIVE

    init {
        viewModelScope.launch {
            TimerService.timerState.collect { state ->
                val currentMode = state.mode
                _timerMode.value = currentMode
                _timerText.value = formatDuration(state.time)

                if (currentMode == TimerMode.OVERTIME && previousMode != TimerMode.OVERTIME && state.activeQuestId != null) {
                    viewModelScope.launch {
                        _questFinishedEvent.emit(state.activeQuestId!!)
                    }
                }
                previousMode = currentMode
            }
        }
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
