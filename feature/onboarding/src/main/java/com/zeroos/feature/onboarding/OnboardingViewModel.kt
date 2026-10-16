package com.zeroos.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.visibleApps
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.ProfileType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Onboarding pages in order (wireframes G1–G6). */
enum class OnboardingPage(val logName: String) {
    INTRO_1("intro 1"),
    INTRO_2("intro 2"),
    INTRO_3("intro 3"),
    DEFAULT_HOME("default home"),
    FAVORITES("favorites"),
    BACKGROUND("background");

    val isIntro get() = this == INTRO_1 || this == INTRO_2 || this == INTRO_3

    companion object {
        val INTRO_PAGES = entries.count { it.isIntro }
    }
}

/** What the phone reported last; checked again whenever onboarding comes back to the front. */
data class SystemStatus(
    val isDefaultHome: Boolean = false,
    val batteryOptimized: Boolean = false,
    val suggested: List<String> = emptyList()
)

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.INTRO_1,
    /** Suggested apps first (F14.3), then all others alphabetically. */
    val apps: List<AppEntry> = emptyList(),
    /** Chosen favorites in the order they were picked. */
    val selected: List<String> = emptyList(),
    val maxFavorites: Int = 0,
    val isDefaultHome: Boolean = false,
    val batteryOptimized: Boolean = false
) {
    val canSelectMore: Boolean get() = selected.size < maxFavorites
}

/**
 * Onboarding (F14): intro → set as default → pick favorites → keep running. Skippable from every page (F14.5)
 * and shown once ([com.zeroos.core.model.settings.LauncherSettings.onboardingCompleted]). Asks for nothing
 * up front (F14.4): the default-home and battery dialogs open only when their buttons are tapped.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    appsRepository: AppsRepository,
    private val settings: SettingsRepository,
    private val system: OnboardingSystem,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val status = MutableStateFlow(SystemStatus())

    val state: StateFlow<OnboardingState> = combine(
        appsRepository.apps,
        settings.settings,
        savedState.getStateFlow(KEY_PAGE, OnboardingPage.INTRO_1),
        savedState.getStateFlow<ArrayList<String>?>(KEY_SELECTED, null),
        status
    ) { apps, current, page, picked, system ->
        val visible = visibleApps(apps, current)
        val suggested = system.suggested.mapNotNull { pkg ->
            visible.firstOrNull { it.key.packageName == pkg && it.profile == ProfileType.MAIN }
        }
        val ordered = suggested + visible.filterNot { it in suggested }
        val max = current.home.maxFavorites
        // Until I change the selection: my current favorites, or else the suggestions.
        val selected = picked ?: current.favorites.ifEmpty { suggested.map { it.key.asString() } }.take(max)
        OnboardingState(page, ordered, selected, max, system.isDefaultHome, system.batteryOptimized)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnboardingState())

    init {
        refreshSystemStatus()
    }

    private val page get() = savedState[KEY_PAGE] ?: OnboardingPage.INTRO_1

    /** Reads default-home and battery status off the main thread; called on start and on every return (F14.2, F14.6). */
    fun refreshSystemStatus() {
        viewModelScope.launch {
            status.value = withContext(ioDispatcher) {
                SystemStatus(system.isDefaultHome(), system.isBatteryOptimized(), system.suggestedPackages())
            }
            logger.info(TAG, "status: default home=${status.value.isDefaultHome}, battery optimized=${status.value.batteryOptimized}")
        }
    }

    fun onPageShown(page: OnboardingPage) = logger.info(TAG, "page: ${page.logName}")

    fun next() {
        val current = page
        if (current == OnboardingPage.FAVORITES) saveFavorites()
        if (current == OnboardingPage.entries.last()) {
            complete("finished")
        } else {
            savedState[KEY_PAGE] = OnboardingPage.entries[current.ordinal + 1]
        }
    }

    /** Back goes to the previous page; nothing happens on the first. */
    fun back() {
        val current = page
        if (current.ordinal > 0) savedState[KEY_PAGE] = OnboardingPage.entries[current.ordinal - 1]
    }

    /** F14.5: straight to home, keeping everything as it is. */
    fun skip() = complete("skipped at ${page.logName}")

    fun toggle(app: AppEntry) {
        val now = state.value
        val key = app.key.asString()
        val next = when {
            key in now.selected -> now.selected - key
            now.canSelectMore -> now.selected + key
            else -> return
        }
        savedState[KEY_SELECTED] = ArrayList(next)
    }

    fun onDefaultHomeResult(isDefault: Boolean) = logger.info(TAG, "default home request: ${if (isDefault) "granted" else "not granted"}")

    fun onOpenFailed(e: Exception) = logger.warn(TAG, "system screen unavailable", e)

    private fun saveFavorites() {
        val selected = state.value.selected
        viewModelScope.launch { settings.update("favorites=${selected.size} apps (onboarding)") { it.copy(favorites = selected) } }
    }

    private fun complete(how: String) {
        logger.info(TAG, "onboarding $how")
        viewModelScope.launch { settings.update("onboardingCompleted=true") { it.copy(onboardingCompleted = true) } }
    }

    private companion object {
        const val TAG = "onboarding"
        const val KEY_PAGE = "onboarding_page"
        const val KEY_SELECTED = "onboarding_selected"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
