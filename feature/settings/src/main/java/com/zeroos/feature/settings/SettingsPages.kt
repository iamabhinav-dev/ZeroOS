package com.zeroos.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.AppListSettings
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.SettingRow
import com.zeroos.core.ui.ToggleRow
import com.zeroos.core.ui.displayName

data class HomeSettingsActions(
    val onEditFavorites: () -> Unit,
    val onMaxFavorites: () -> Unit,
    val onAlignment: () -> Unit,
    val onShowBattery: (Boolean) -> Unit,
    val onShowNextAlarm: (Boolean) -> Unit
)

/** Wireframe F2 (F1.3, F1.7, F1.8, F3). */
@Composable
fun HomeSettingsScreen(state: HomeSettings, actions: HomeSettingsActions, onBack: () -> Unit) {
    SettingsPageScaffold(stringResource(R.string.settings_home), SettingsTestTags.HOME, onBack) {
        item { NavRow(R.string.home_settings_edit_favorites, actions.onEditFavorites) }
        item { SettingRow(stringResource(R.string.home_settings_max_favorites), state.maxFavorites.toString(), actions.onMaxFavorites) }
        item { SettingRow(stringResource(R.string.home_settings_alignment), stringResource(state.alignment.label()), actions.onAlignment) }
        item { ToggleRow(stringResource(R.string.home_settings_show_battery), state.showBattery, actions.onShowBattery) }
        item { ToggleRow(stringResource(R.string.home_settings_show_next_alarm), state.showNextAlarm, actions.onShowNextAlarm) }
    }
}

data class AppListSettingsActions(
    val onKeyboardAutoOpen: (Boolean) -> Unit,
    val onAutoLaunch: (Boolean) -> Unit,
    val onFastScroller: (Boolean) -> Unit,
    val onHiddenApps: () -> Unit,
    val onConfirmation: () -> Unit
)

/** Wireframe F3 (F2.3, F2.5, F2.7, F5.2, F5.3). */
@Composable
fun AppListSettingsScreen(state: AppListSettings, hiddenCount: Int, actions: AppListSettingsActions, onBack: () -> Unit) {
    SettingsPageScaffold(stringResource(R.string.settings_app_list), SettingsTestTags.APP_LIST, onBack) {
        item { ToggleRow(stringResource(R.string.app_list_settings_keyboard), state.keyboardAutoOpen, actions.onKeyboardAutoOpen) }
        item { ToggleRow(stringResource(R.string.app_list_settings_auto_launch), state.autoLaunchSingleMatch, actions.onAutoLaunch) }
        item { ToggleRow(stringResource(R.string.app_list_settings_scroller), state.fastScroller, actions.onFastScroller) }
        item { NavRow(stringResource(R.string.app_list_settings_hidden, hiddenCount), actions.onHiddenApps) }
        item {
            SettingRow(
                stringResource(R.string.app_list_settings_confirmation),
                state.hiddenAppsConfirmation.label(),
                actions.onConfirmation
            )
        }
    }
}

data class GesturesActions(
    val onSwipeLeft: () -> Unit,
    val onSwipeRight: () -> Unit,
    val onDoubleTapLock: (Boolean) -> Unit = {},
    /** Opens the Accessibility explanation (Step 4.1). */
    val onTurnOnAccessibility: () -> Unit = {}
)

/**
 * Wireframes F4 and H13 (F4.2–F4.6). Swipe up is always the app list (F4.1); swipe down opens the notification
 * shade while Accessibility runs, otherwise search (F4.4, D-033), so neither is a setting. Double-tap to lock
 * needs Accessibility; a warning shows when it's off, or on but stopped by the phone (Step 4.2).
 */
@Composable
fun GesturesScreen(state: SettingsUiState, actions: GesturesActions, onBack: () -> Unit) {
    val off = stringResource(R.string.common_off)
    val doubleTap = state.settings.gestures.doubleTapLock
    SettingsPageScaffold(stringResource(R.string.settings_gestures), SettingsTestTags.GESTURES, onBack) {
        item { SettingRow(stringResource(R.string.gestures_swipe_left), state.swipeLeftApp?.displayName() ?: off, actions.onSwipeLeft) }
        item { SettingRow(stringResource(R.string.gestures_swipe_right), state.swipeRightApp?.displayName() ?: off, actions.onSwipeRight) }
        item { ToggleRow(stringResource(R.string.gestures_double_tap), doubleTap, actions.onDoubleTapLock) }
        when {
            doubleTap && !state.accessibilityEnabled -> item {
                MinimalListItem(stringResource(R.string.gestures_accessibility_off), onClick = actions.onTurnOnAccessibility, muted = true)
            }

            state.accessibilityEnabled && !state.accessibilityRunning -> item {
                BodyText(stringResource(R.string.gestures_accessibility_stopped), muted = true, small = true)
            }
        }
    }
}

private fun Alignment.label() = when (this) {
    Alignment.LEFT -> R.string.alignment_left
    Alignment.CENTER -> R.string.alignment_center
    Alignment.RIGHT -> R.string.alignment_right
}

@Composable
private fun HiddenAppsConfirmation.label() = when (this) {
    HiddenAppsConfirmation.OFF -> stringResource(R.string.common_off)
    HiddenAppsConfirmation.CONFIRM -> stringResource(R.string.hidden_confirmation_confirm)
    HiddenAppsConfirmation.DELAY -> stringResource(R.string.hidden_confirmation_delay, HIDDEN_APPS_DELAY_SECONDS)
}
