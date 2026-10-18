package com.zeroos.feature.home

import com.zeroos.core.data.apps.AppsRepository
import com.zeroos.core.data.apps.HomeAppsCache
import com.zeroos.core.data.apps.homeAppsToCache
import com.zeroos.core.data.focus.FocusSession
import com.zeroos.core.data.wellbeing.LaunchGate
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.LauncherSettings
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Home's app list with the cold-start cache (NFR-P2), for [HomeViewModel]. */
class HomeApps @Inject constructor(
    private val repository: AppsRepository,
    private val cache: HomeAppsCache,
    private val io: CoroutineDispatcher,
    private val gate: LaunchGate
) {
    fun snapshots(): Flow<HomeAppsSnapshot> = homeAppsWithCache(cache, repository.apps, io)

    /** Starts the app (F1.4), or shows its stop screen first (F8.1, F8.4); false if it can't be started. */
    fun launch(app: AppEntry): Boolean = gate.launch(app)

    /** The running focus session (F9.3, F9.4). */
    val focus: Flow<FocusSession?> = gate.focus

    fun saver(scope: CoroutineScope) = HomeAppsSaver(cache, scope, io)
}

/** Apps for home; [live] is false while they still come from [HomeAppsCache]. */
data class HomeAppsSnapshot(val apps: List<AppEntry>, val live: Boolean)

/**
 * Cold start (NFR-P2): first the apps home showed last time, read from [cache] off the main thread, then the
 * real list from [live] as soon as it has loaded.
 */
fun homeAppsWithCache(cache: HomeAppsCache, live: Flow<List<AppEntry>>, io: CoroutineDispatcher): Flow<HomeAppsSnapshot> = flow {
    emit(HomeAppsSnapshot(withContext(io) { cache.read() }.orEmpty(), live = false))
    emitAll(live.map { HomeAppsSnapshot(it, live = true) })
}

/** Saves what home shows for the next cold start, only from the real app list and only when it changed. */
class HomeAppsSaver(private val cache: HomeAppsCache, private val scope: CoroutineScope, private val io: CoroutineDispatcher) {
    private var saved: List<AppEntry>? = null

    fun onShown(snapshot: HomeAppsSnapshot, settings: LauncherSettings) {
        if (!snapshot.live) return
        val apps = homeAppsToCache(snapshot.apps, settings)
        if (apps == saved) return
        saved = apps
        scope.launch(io) { cache.write(apps) }
    }
}
