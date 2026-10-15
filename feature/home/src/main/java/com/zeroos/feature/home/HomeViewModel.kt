package com.zeroos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Home screen state (Step 1.3): the current time. Favorites, stats and focus arrive in later phases. */
@HiltViewModel
class HomeViewModel @Inject constructor(timeTicks: TimeTicks) : ViewModel() {
    val time: StateFlow<ZonedDateTime> = timeTicks.ticks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), timeTicks.now())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
