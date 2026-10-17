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

/** What the phone wallpaper screens show (F6.8, wireframes F5b and F5c). */
data class PhoneWallpaperState(
    /** Gallery of the current theme (D-038). */
    val items: List<WallpaperItem> = emptyList(),
    /** The wallpaper open full screen, or null while the gallery shows. */
    val previewing: WallpaperItem? = null,
    /** The Apply dialog (Home screen / Lock screen / Both) is open. */
    val choosing: Boolean = false,
    val history: PhoneWallpaperHistory = PhoneWallpaperHistory(),
    val result: WallpaperOutcome? = null,
    val busy: Boolean = false
) {
    /** Outlined in the gallery: what ZeroOS last set on the home screen. */
    val currentHomeId: String? get() = history.current(WallpaperScreen.HOME)
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
        savedState.getStateFlow<String?>(KEY_PREVIEW, null)
    ) { state, history, previewId ->
        state.copy(history = history, previewing = state.items.firstOrNull { it.id == previewId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhoneWallpaperState())

    /** The screen tells which theme is showing; the gallery follows it (D-038). */
    fun setTone(tone: WallpaperTone) {
        viewModelScope.launch {
            val items = wallpapers.gallery(tone)
            local.value = local.value.copy(items = items)
        }
    }

    /** Tapping a thumbnail opens it full screen (wireframe F5c). */
    fun open(item: WallpaperItem) {
        savedState[KEY_PREVIEW] = item.id
        local.value = local.value.copy(result = null)
    }

    /** Back arrow or Back gesture: return to the gallery without changes. */
    fun closePreview() {
        savedState[KEY_PREVIEW] = null
        local.value = local.value.copy(choosing = false)
    }

    fun showApply() {
        local.value = local.value.copy(choosing = true)
    }

    fun dismissApply() {
        local.value = local.value.copy(choosing = false)
    }

    /** Sets the previewed wallpaper on [screens] only; the gallery then shows the result line. */
    fun apply(screens: Set<WallpaperScreen>) {
        val item = state.value.previewing ?: return
        closePreview()
        run { WallpaperOutcome.Set(wallpapers.apply(item, screens)) }
    }

    /** Only [screen] changes; says so if the previous image has been removed from the gallery. */
    fun restorePrevious(screen: WallpaperScreen) {
        val previous = state.value.history.previous(screen) ?: return
        run { wallpapers.restorePrevious(screen, previous)?.let { WallpaperOutcome.Set(it) } ?: WallpaperOutcome.Gone }
    }

    suspend fun thumbnail(item: WallpaperItem, width: Int, height: Int): Bitmap = wallpapers.thumbnail(item, width, height)

    private fun run(block: suspend () -> WallpaperOutcome) {
        local.value = local.value.copy(busy = true, result = null)
        viewModelScope.launch {
            val outcome = block()
            local.value = local.value.copy(busy = false, result = outcome)
        }
    }

    private companion object {
        const val KEY_PREVIEW = "phone_wallpaper_preview"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
