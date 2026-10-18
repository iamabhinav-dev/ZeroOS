package com.zeroos.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zeroos.core.data.wellbeing.Overlay
import com.zeroos.core.data.wellbeing.StopScreen
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.feature.wellbeing.BreakScreen
import com.zeroos.feature.wellbeing.StopActions
import com.zeroos.feature.wellbeing.StopUiState

/**
 * The full-screen window the Accessibility service draws over other apps (TYPE_ACCESSIBILITY_OVERLAY, no extra
 * permission). Back is caught: on the limit screen it means "Go home", on the break screen it does nothing (F8.16).
 */
class OverlayWindow(private val service: AccessibilityService) : OverlayDisplay {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var root: FrameLayout? = null
    private var owner: OverlayOwner? = null
    override var showingFor: String? = null
        private set

    override fun show(content: OverlayContent, actions: OverlayActions) {
        hide()
        val owner = OverlayOwner()
        val root = object : FrameLayout(service) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode != KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
                if (event.action == KeyEvent.ACTION_UP && content.overlay is Overlay.Limit) actions.onGoHome()
                return true
            }
        }
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        root.addView(ComposeView(service).apply { setContent { OverlayScreen(content, actions) } })
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        windowManager.addView(root, params)
        this.root = root
        this.owner = owner
        showingFor = content.overlay.packageName
    }

    override fun hide() {
        root?.let(windowManager::removeView)
        owner?.destroy()
        root = null
        owner = null
        showingFor = null
    }

    override fun goHome() {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
}

/** H7b (limit, same screen as inside ZeroOS) or H14 (break), in the user's theme and text size. */
@Composable
fun OverlayScreen(content: OverlayContent, actions: OverlayActions) {
    ZeroTheme(content.appearance.theme, content.appearance.textSize) {
        when (val overlay = content.overlay) {
            is Overlay.Limit -> com.zeroos.feature.wellbeing.StopScreen(
                StopUiState(content.app, StopScreen.Limit(overlay.usedMinutes, overlay.extrasLeft), content.pun),
                StopActions(onOpen = {}, onMoreMinutes = actions.onMoreMinutes, onBack = actions.onGoHome)
            )

            is Overlay.Break -> BreakScreen(content.pun, content.buttonPun, overlay.waitSeconds, actions.onBreakDone)
        }
    }
}

/** Lifecycle and saved state for a Compose view outside an activity; resumed while the overlay is up. */
private class OverlayOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    init {
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
