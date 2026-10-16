package com.zeroos.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.AppListSettings
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.resources.R
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.SettingRow
import com.zeroos.core.ui.ToggleRow

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

data class GesturesActions(val onSwipeLeft: () -> Unit, val onSwipeRight: () -> Unit)

/**
 * Wireframe F4 (F4.2, F4.3, F4.6). Swipe up is always the app list (F4.1) and swipe down always opens search, or
 * the notification shade once the Accessibility service exists (F4.4, D-033), so neither is shown. Double-tap to
 * lock needs that service too (Phase 4), so it's shown but disabled.
 */
@Composable
fun GesturesScreen(state: SettingsUiState, actions: GesturesActions, onBack: () -> Unit) {
    val off = stringResource(R.string.common_off)
    SettingsPageScaffold(stringResource(R.string.settings_gestures), SettingsTestTags.GESTURES, onBack) {
        item { SettingRow(stringResource(R.string.gestures_swipe_left), state.swipeLeftLabel ?: off, actions.onSwipeLeft) }
        item { SettingRow(stringResource(R.string.gestures_swipe_right), state.swipeRightLabel ?: off, actions.onSwipeRight) }
        item { UnavailableRow(stringResource(R.string.gestures_double_tap), stringResource(R.string.gestures_needs_accessibility)) }
    }
}

/** A row that can't be changed yet: muted and announced as disabled. */
@Composable
private fun UnavailableRow(label: String, value: String) = MinimalListItem(
    text = stringResource(R.string.settings_label_value, label, value),
    onClick = {},
    muted = true,
    modifier = Modifier.semantics { disabled() }
)

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
