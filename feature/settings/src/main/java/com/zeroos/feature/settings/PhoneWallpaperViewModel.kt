package com.zeroos.feature.settings

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.wallpaper.PhoneWallpapers
import com.zeroos.core.data.wallpaper.WallpaperItem
import com.zeroos.core.data.wallpaper.WallpaperResult
import com.zeroos.core.data.wallpaper.WallpaperTone
import com.zeroos.core.model.settings.PhoneWallpaperHistory
import com.zeroos.core.model.settings.WallpaperScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the phone wallpaper screen shows (F6.8, wireframe F5b). */
data class PhoneWallpaperState(
    /** Gallery of the current theme (D-038). */
    val items: List<WallpaperItem> = emptyList(),
    val selectedId: String? = null,
    val history: PhoneWallpaperHistory = PhoneWallpaperHistory(),
    /** Set while the "also the other screen?" question is open: the screen that was chosen. */
    val asking: WallpaperScreen? = null,
    val result: WallpaperOutcome? = null,
    val busy: Boolean = false
) {
    val selected: WallpaperItem? get() = items.firstOrNull { it.id == selectedId } ?: items.firstOrNull()
}

/** The last action's result line(s). */
sealed interface WallpaperOutcome {
    data class Set(val result: WallpaperResult) : WallpaperOutcome

    /** "Restore previous" pointed at an image that has since been removed. */
    data object Gone : WallpaperOutcome
}

/** Settings → Appearance → Phone wallpaper (F6.8). */
@HiltViewModel
class PhoneWallpaperViewModel @Inject constructor(
    private val wallpapers: PhoneWallpapers,
    settings: SettingsRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val local = MutableStateFlow(PhoneWallpaperState())

    val state: StateFlow<PhoneWallpaperState> = combine(
        local,
        settings.settings.map { it.phoneWallpapers },
        savedState.getStateFlow<String?>(KEY_SELECTED, null)
    ) { state, history, selected ->
        val pick = selected?.takeIf { id -> state.items.any { it.id == id } }
            ?: history.current(WallpaperScreen.HOME)?.takeIf { id -> state.items.any { it.id == id } }
        state.copy(history = history, selectedId = pick)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhoneWallpaperState())

    /** The screen tells which theme is showing; the gallery follows it (D-038). */
    fun setTone(tone: WallpaperTone) {
        viewModelScope.launch {
            val items = wallpapers.gallery(tone)
            local.value = local.value.copy(items = items)
        }
    }

    fun select(item: WallpaperItem) {
        savedState[KEY_SELECTED] = item.id
    }

    /** "Set on home screen" / "Set on lock screen": first ask about the other screen. */
    fun chooseScreen(screen: WallpaperScreen) {
        local.value = local.value.copy(asking = screen, result = null)
    }

    fun cancelAsk() {
        local.value = local.value.copy(asking = null)
    }

    /** Answer to "Also use it on the other screen?": [both] false leaves the other screen unchanged. */
    fun confirm(both: Boolean) {
        val screen = local.value.asking ?: return
        val item = state.value.selected ?: return
        val screens = if (both) WallpaperScreen.entries.toSet() else setOf(screen)
        run { WallpaperOutcome.Set(wallpapers.apply(item, screens)) }
    }

    /** Only [screen] changes; says so if the previous image has been removed from the gallery. */
    fun restorePrevious(screen: WallpaperScreen) {
        val previous = state.value.history.previous(screen) ?: return
        run { wallpapers.restorePrevious(screen, previous)?.let { WallpaperOutcome.Set(it) } ?: WallpaperOutcome.Gone }
    }

    suspend fun thumbnail(item: WallpaperItem, width: Int, height: Int): Bitmap = wallpapers.thumbnail(item, width, height)

    private fun run(block: suspend () -> WallpaperOutcome) {
        local.value = local.value.copy(asking = null, busy = true, result = null)
        viewModelScope.launch {
            val outcome = block()
            local.value = local.value.copy(busy = false, result = outcome)
        }
    }

    private companion object {
        const val KEY_SELECTED = "phone_wallpaper_selected"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
