package neth.iecal.questphone.ui.screens.stats.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsInfo

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val statsDao = StatsDatabaseProvider.getInstance(application).statsDao()

    val allStats: StateFlow<List<StatsInfo>> = statsDao.getAllStatsForUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
