package com.zeroos.launcher.accessibility

import com.zeroos.core.data.wellbeing.Overlay
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.settings.AppearanceSettings
import com.zeroos.core.model.settings.YOUTUBE_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneScreenTest {
    /** The real window: records every show and hide. */
    private class FakeWindow : OverlayDisplay {
        val calls = mutableListOf<String>()
        var current: Overlay? = null
        override val showingFor get() = current?.packageName

        override fun show(content: OverlayContent, actions: OverlayActions) {
            current = content.overlay
            calls += "show ${content.overlay::class.simpleName}"
        }

        override fun hide() {
            current = null
            calls += "hide"
        }

        override fun goHome() {
            calls += "home"
        }
    }

    private val window = FakeWindow()
    private val screen = OneScreen(window)
    private val actions = OverlayActions(onGoHome = {})
    private val app = AppEntry(AppKey(YOUTUBE_PACKAGE, "Main", 0), "YouTube")
    private fun content(overlay: Overlay) = OverlayContent(overlay, app, 0, 0, AppearanceSettings())
    private val limit = content(Overlay.Limit(YOUTUBE_PACKAGE, usedMinutes = 30, extrasLeft = 2))
    private val pause = content(Overlay.Break(YOUTUBE_PACKAGE, waitSeconds = 5))
    private val shorts = content(Overlay.ShortForm(YOUTUBE_PACKAGE, allowLeft = 1, autoHomeSeconds = null))

    @Test
    fun f16_7_shortVideoBlockWaitsWhileTheBreakScreenIsUp() {
        screen.wellbeing.show(pause, actions)
        screen.shortForm.show(shorts, actions)
        // The break screen isn't redrawn, so its countdown isn't restarted.
        assertEquals(listOf("show Break"), window.calls)
        assertEquals(YOUTUBE_PACKAGE, screen.shortForm.showingFor)
        // Break over, still in Shorts: the short-video screen takes its place.
        screen.wellbeing.hide()
        assertEquals(listOf("show Break", "show ShortForm"), window.calls)
        assertNull(screen.wellbeing.showingFor)
    }

    @Test
    fun f8_7_limitReplacesTheShortVideoScreenWhichComesBackAfterwards() {
        screen.shortForm.show(shorts, actions)
        screen.wellbeing.show(limit, actions)
        assertEquals(Overlay.Limit(YOUTUBE_PACKAGE, 30, 2), window.current)
        // "5 more minutes" while still in Shorts: the short-video block is back.
        screen.wellbeing.hide()
        assertEquals(Overlay.ShortForm(YOUTUBE_PACKAGE, 1, null), window.current)
        // Left Shorts: nothing over the app.
        screen.shortForm.hide()
        assertNull(window.current)
        assertEquals(listOf("show ShortForm", "show Limit", "show ShortForm", "hide"), window.calls)
    }

    @Test
    fun f16_7_shortVideoGoingAwayUnderTheLimitLeavesTheLimit() {
        screen.wellbeing.show(limit, actions)
        screen.shortForm.show(shorts, actions)
        screen.shortForm.hide()
        screen.shortForm.goHome()
        assertEquals(listOf("show Limit", "home"), window.calls)
        assertEquals(Overlay.Limit(YOUTUBE_PACKAGE, 30, 2), window.current)
    }
}
