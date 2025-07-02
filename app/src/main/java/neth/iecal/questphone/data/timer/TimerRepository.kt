package neth.iecal.questphone.data.timer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_settings")

enum class TimerMode {
    QUEST_COUNTDOWN,
    OVERTIME,
    BREAK,
    UNSCHEDULED_BREAK,
    INACTIVE
}

data class TimerState(
    val startTimeMillis: Long,
    val timerMode: TimerMode,
    val questEndTimeMillis: Long
)

class TimerRepository(private val context: Context) {

    private object PreferencesKeys {
        val START_TIME_MILLIS = longPreferencesKey("start_time_millis")
        val TIMER_MODE = stringPreferencesKey("timer_mode")
        val QUEST_END_TIME_MILLIS = longPreferencesKey("quest_end_time_millis")
    }

    val timerStateFlow: Flow<TimerState> = context.dataStore.data.map { preferences ->
        val startTime = preferences[PreferencesKeys.START_TIME_MILLIS] ?: System.currentTimeMillis()
        val timerMode = TimerMode.valueOf(preferences[PreferencesKeys.TIMER_MODE] ?: TimerMode.INACTIVE.name)
        val questEndTime = preferences[PreferencesKeys.QUEST_END_TIME_MILLIS] ?: 0L
        TimerState(startTime, timerMode, questEndTime)
    }

    suspend fun saveTimerState(startTime: Long, timerMode: TimerMode, questEndTime: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.START_TIME_MILLIS] = startTime
            preferences[PreferencesKeys.TIMER_MODE] = timerMode.name
            preferences[PreferencesKeys.QUEST_END_TIME_MILLIS] = questEndTime
        }
    }
}
