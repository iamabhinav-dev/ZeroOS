package com.zeroos.launcher.accessibility

/**
 * The one overlay window shared by the screen-time lock (F19.3), wellbeing (limit, break; F8.7, F8.16) and the
 * short-video blocker (F16.7), so only one screen is ever shown. The lock covers everything, so it wins; then the
 * limit or break screen, which covers the whole app; a short-video block asked for meanwhile waits and appears when
 * the screens above it go, if it's still wanted.
 */
class OneScreen(private val window: OverlayDisplay) {
    private class Screen(val content: OverlayContent, val actions: OverlayActions)

    private enum class Level { LOCK, WELLBEING, SHORT_FORM }

    private val screens = mutableMapOf<Level, Screen>()
    private var shown: Screen? = null

    /** For the screen-time lock: always shown at once (F19.3). */
    val lock: OverlayDisplay = Slot(Level.LOCK)

    /** For [OutsideEnforcer]: shown unless the lock is up. */
    val wellbeing: OverlayDisplay = Slot(Level.WELLBEING)

    /** For the short-video blocker: shown only while no lock, limit or break screen is up. */
    val shortForm: OverlayDisplay = Slot(Level.SHORT_FORM)

    /** Touches the window only when the visible screen changes, so a waiting request never restarts a countdown. */
    private fun refresh() {
        val next = Level.entries.firstNotNullOfOrNull { screens[it] }
        if (next === shown) return
        shown = next
        if (next == null) window.hide() else window.show(next.content, next.actions)
    }

    private inner class Slot(private val level: Level) : OverlayDisplay {
        override val showingFor: String?
            get() = screens[level]?.content?.overlay?.packageName

        override fun show(content: OverlayContent, actions: OverlayActions) = set(Screen(content, actions))

        override fun hide() = set(null)

        override fun goHome() = window.goHome()

        private fun set(screen: Screen?) {
            if (screen == null) screens.remove(level) else screens[level] = screen
            refresh()
        }
    }
}
