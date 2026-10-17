package com.zeroos.core.data.wallpaper

import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import com.zeroos.core.model.settings.Wallpaper
import com.zeroos.core.model.settings.WallpaperScreen
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** What happened when a wallpaper was set (F6.8). */
data class WallpaperResult(
    val accepted: Set<WallpaperScreen> = emptySet(),
    /** Screens Android didn't accept, e.g. a phone without a separate lock-screen wallpaper. */
    val rejected: Set<WallpaperScreen> = emptySet(),
    /** The image couldn't be read or setting wasn't allowed; nothing changed. */
    val failed: Boolean = false,
    /** Home background was "Black" and was switched to the phone wallpaper so the new one shows (D-038). */
    val homeBackgroundSwitched: Boolean = false
)

/**
 * Sets the phone's wallpapers from the built-in gallery (F6.8, D-038). Decoding and setting run on [io]. Each
 * accepted screen is recorded for "Restore previous"; screens not asked for are never touched.
 */
class PhoneWallpapers(
    private val settings: SettingsRepository,
    private val system: WallpaperSystem,
    private val io: CoroutineDispatcher,
    private val logger: Logger
) {
    suspend fun gallery(tone: WallpaperTone): List<WallpaperItem> = withContext(io) {
        WallpaperCatalog.forTone(system.files(), tone)
    }

    /** The gallery item with [id], in any tone, or null if its image is gone. */
    suspend fun find(id: String): WallpaperItem? = withContext(io) { WallpaperCatalog.find(system.files(), id) }

    suspend fun apply(item: WallpaperItem, screens: Set<WallpaperScreen>): WallpaperResult {
        val accepted = setOrNull(item, screens) ?: return WallpaperResult(failed = true)
        val rejected = screens - accepted
        var switched = false
        settings.update("phoneWallpaper=${item.id} on ${accepted.names()}") { current ->
            var next = current
            accepted.forEach { next = next.copy(phoneWallpapers = next.phoneWallpapers.withSet(it, item.id)) }
            if (WallpaperScreen.HOME in accepted && next.appearance.wallpaper == Wallpaper.BLACK) {
                switched = true
                next = next.copy(appearance = next.appearance.copy(wallpaper = Wallpaper.SYSTEM))
            }
            next
        }
        if (rejected.isNotEmpty()) logger.warn(TAG, "wallpaper ${item.id} not accepted on ${rejected.names()}")
        return WallpaperResult(accepted, rejected, homeBackgroundSwitched = switched)
    }

    /** Sets the wallpaper ZeroOS set on [screen] before the current one; only that screen changes. */
    suspend fun restorePrevious(screen: WallpaperScreen, previousId: String): WallpaperResult {
        val item = find(previousId) ?: return WallpaperResult(rejected = setOf(screen)).also {
            logger.warn(TAG, "previous wallpaper $previousId no longer available")
        }
        logger.info(TAG, "restoring previous wallpaper on ${screen.name.lowercase()}")
        return apply(item, setOf(screen))
    }

    /** The accepted screens, or null (logged) if the image couldn't be read or setting wasn't allowed. */
    private suspend fun setOrNull(item: WallpaperItem, screens: Set<WallpaperScreen>): Set<WallpaperScreen>? = try {
        withContext(io) { system.set(item, screens) }
    } catch (e: IOException) {
        logger.warn(TAG, "wallpaper ${item.id} could not be set", e)
        null
    } catch (e: SecurityException) {
        logger.warn(TAG, "wallpaper ${item.id} could not be set", e)
        null
    }

    private fun Set<WallpaperScreen>.names() = if (isEmpty()) "none" else joinToString(",") { it.name.lowercase() }

    private companion object {
        const val TAG = "wallpaper"
    }
}
