package com.zeroos.core.data.wellbeing

import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.focus.FocusRepository
import com.zeroos.core.data.focus.FocusSession
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.LauncherSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Every launch from home or the app list goes through here (F8.1, F8.4, F9.3). During focus only allowed apps
 * open. Apps without a wellbeing rule open at once; for the others [stopFor] is set and the stop screen decides,
 * after reading today's usage off the main thread, whether to pause, block or just open.
 */
class LaunchGate(
    private val apps: AppsRepository,
    settings: SettingsRepository,
    focusRepository: FocusRepository,
    scope: CoroutineScope,
    private val logger: Logger
) {
    private val current: StateFlow<LauncherSettings?> = settings.settings.stateIn(scope, SharingStarted.Eagerly, null)

    /** The running focus session, or null (F9.3): home and the app list show only its allowed apps. */
    val focus: StateFlow<FocusSession?> = focusRepository.active.stateIn(scope, SharingStarted.Eagerly, null)
    private val pending = MutableStateFlow<AppEntry?>(null)

    /** The app waiting behind a stop screen, or null. */
    val stopFor: StateFlow<AppEntry?> = pending.asStateFlow()

    /** Opens [app], or hands it to the stop screen. True if it opened or the stop screen will show. */
    fun launch(app: AppEntry): Boolean {
        val session = focus.value
        val pkg = app.key.packageName
        return when {
            session != null && !session.allows(app.key.asString()) -> false.also { logger.info(TAG, "focus: not allowed $pkg") }

            // Settings not loaded yet (the first moment after a cold start): nothing to check against, open it.
            current.value?.wellbeing?.mayStop(pkg) != true -> apps.launch(app.key)

            else -> true.also {
                logger.info(TAG, "stop check for $pkg")
                pending.value = app
            }
        }
    }

    /** Opens the app behind the stop screen: "Open anyway", "5 more minutes" or not stopped after all. */
    fun open(app: AppEntry): Boolean {
        pending.value = null
        return apps.launch(app.key)
    }

    /** "Go back" / "Go home": the app stays closed. */
    fun dismiss() {
        pending.value = null
    }

    private companion object {
        const val TAG = "wellbeing"
    }
}
