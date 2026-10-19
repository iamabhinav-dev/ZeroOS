package com.zeroos.launcher.accessibility

/**
 * The one overlay window shared by wellbeing (limit, break; F8.7, F8.16) and the short-video blocker (F16.7), so
 * only one screen is ever shown. The limit or break screen covers the whole app, so it wins; a short-video block
 * asked for meanwhile waits and appears when that screen goes, if it's still wanted.
 */
class OneScreen(private val window: OverlayDisplay) {
    private class Screen(val content: OverlayContent, val actions: OverlayActions)

    private var wellbeingScreen: Screen? = null
    private var shortFormScreen: Screen? = null
    private var shown: Screen? = null

    /** For [OutsideEnforcer]: always shown at once. */
    val wellbeing: OverlayDisplay = Slot(wins = true)

    /** For the short-video blocker: shown only while no limit or break screen is up. */
    val shortForm: OverlayDisplay = Slot(wins = false)

    /** Touches the window only when the visible screen changes, so a waiting request never restarts a countdown. */
    private fun refresh() {
        val next = wellbeingScreen ?: shortFormScreen
        if (next === shown) return
        shown = next
        if (next == null) window.hide() else window.show(next.content, next.actions)
    }

    private inner class Slot(private val wins: Boolean) : OverlayDisplay {
        override val showingFor: String?
            get() = (if (wins) wellbeingScreen else shortFormScreen)?.content?.overlay?.packageName

        override fun show(content: OverlayContent, actions: OverlayActions) = set(Screen(content, actions))

        override fun hide() = set(null)

        override fun goHome() = window.goHome()

        private fun set(screen: Screen?) {
            if (wins) wellbeingScreen = screen else shortFormScreen = screen
            refresh()
        }
    }
}
