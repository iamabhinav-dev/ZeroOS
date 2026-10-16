package com.zeroos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.settings.AppearanceSettings
import com.zeroos.core.model.settings.TextSize
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.model.settings.Wallpaper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Appearance settings (F6, wireframe F5). Each row cycles through its options when tapped. */
@HiltViewModel
class AppearanceViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {

    val state: StateFlow<AppearanceSettings> = settings.settings.map { it.appearance }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppearanceSettings())

    fun nextTheme() = change { it.copy(theme = it.theme.next()) }

    fun nextWallpaper() = change { it.copy(wallpaper = it.wallpaper.next()) }

    fun nextTextSize() = change { it.copy(textSize = it.textSize.next()) }

    fun setShowClock(show: Boolean) = change { it.copy(showClock = show) }

    fun setShowSeconds(show: Boolean) = change { it.copy(showSeconds = show) }

    fun setShowDate(show: Boolean) = change { it.copy(showDate = show) }

    fun setShowStatusBar(show: Boolean) = change { it.copy(showStatusBar = show) }

    private fun change(transform: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch {
            val next = transform(state.value)
            settings.update("appearance=$next") { it.copy(appearance = transform(it.appearance)) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun ThemeMode.next() = ThemeMode.entries[(ordinal + 1) % ThemeMode.entries.size]

private fun Wallpaper.next() = Wallpaper.entries[(ordinal + 1) % Wallpaper.entries.size]

private fun TextSize.next() = TextSize.entries[(ordinal + 1) % TextSize.entries.size]
