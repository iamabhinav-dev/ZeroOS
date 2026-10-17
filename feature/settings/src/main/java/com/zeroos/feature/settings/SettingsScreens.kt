package com.zeroos.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.model.settings.AppListSettings
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import com.zeroos.core.model.settings.HomeSettings
import com.zeroos.core.resources.R
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.PermissionRequest
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.SettingRow
import com.zeroos.core.ui.ToggleRow
import com.zeroos.core.ui.ZeroTheme

/** Pages inside Settings (wireframes F1–F7, E3, E4). Back goes to [parent]; from [MAIN] it closes Settings. */
enum class SettingsPage(val logName: String) {
    MAIN("main"),
    HOME("home"),
    FAVORITES("favorites"),
    APP_LIST("app list"),
    HIDDEN_APPS("hidden apps"),
    GESTURES("gestures"),
    PICK_SWIPE_LEFT("swipe left app"),
    PICK_SWIPE_RIGHT("swipe right app"),
    APPEARANCE("appearance"),
    PHONE_WALLPAPER("phone wallpaper"),
    BACKUP("backup"),
    ABOUT("about"),
    LICENSES("licenses");

    val parent: SettingsPage
        get() = when (this) {
            FAVORITES -> HOME
            HIDDEN_APPS -> APP_LIST
            PICK_SWIPE_LEFT, PICK_SWIPE_RIGHT -> GESTURES
            LICENSES -> ABOUT
            PHONE_WALLPAPER -> APPEARANCE
            else -> MAIN
        }
}

/**
 * Settings, opened by long-pressing home (F1.9). Diagnostics lives in the app module, so it's opened through
 * [onOpenDiagnostics].
 */
@Composable
fun SettingsRoute(onClose: () -> Unit, onOpenDiagnostics: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(page) { viewModel.onPageShown(page) }
    val back = { if (page == SettingsPage.MAIN) onClose() else page = page.parent }

    when (page) {
        SettingsPage.MAIN -> SettingsMainScreen(
            onOpen = { page = it },
            onOpenDiagnostics = onOpenDiagnostics,
            onBack = back
        )

        SettingsPage.HOME -> HomeSettingsScreen(
            state = state.settings.home,
            actions = HomeSettingsActions(
                onEditFavorites = { page = SettingsPage.FAVORITES },
                onMaxFavorites = { viewModel.cycle(SettingCycle.MAX_FAVORITES) },
                onAlignment = { viewModel.cycle(SettingCycle.ALIGNMENT) },
                onShowBattery = { viewModel.setToggle(SettingToggle.SHOW_BATTERY, it) },
                onShowNextAlarm = { viewModel.setToggle(SettingToggle.SHOW_NEXT_ALARM, it) }
            ),
            onBack = back
        )

        SettingsPage.FAVORITES -> FavoritesEditorRoute(onBack = back)

        SettingsPage.APP_LIST -> AppListSettingsScreen(
            state = state.settings.appList,
            hiddenCount = state.hiddenCount,
            actions = AppListSettingsActions(
                onKeyboardAutoOpen = { viewModel.setToggle(SettingToggle.KEYBOARD_AUTO_OPEN, it) },
                onAutoLaunch = { viewModel.setToggle(SettingToggle.AUTO_LAUNCH_SINGLE_MATCH, it) },
                onFastScroller = { viewModel.setToggle(SettingToggle.FAST_SCROLLER, it) },
                onHiddenApps = { page = SettingsPage.HIDDEN_APPS },
                onConfirmation = { viewModel.cycle(SettingCycle.HIDDEN_APPS_CONFIRMATION) }
            ),
            onBack = back
        )

        SettingsPage.HIDDEN_APPS -> HiddenAppsRoute(onBack = back)

        SettingsPage.GESTURES -> GesturesRoute(state, viewModel, onOpen = { page = it }, onBack = back)

        SettingsPage.PICK_SWIPE_LEFT -> SwipeAppPicker(SwipeSide.LEFT, state, viewModel, onDone = back)

        SettingsPage.PICK_SWIPE_RIGHT -> SwipeAppPicker(SwipeSide.RIGHT, state, viewModel, onDone = back)

        else -> SimplePage(page, onOpen = { page = it }, onBack = back)
    }
}

/** Pages that only open other pages or go back: Appearance, Phone wallpaper, Backup, About, Licenses. */
@Composable
private fun SimplePage(page: SettingsPage, onOpen: (SettingsPage) -> Unit, onBack: () -> Unit) {
    when (page) {
        SettingsPage.APPEARANCE -> AppearanceRoute(onPhoneWallpaper = { onOpen(SettingsPage.PHONE_WALLPAPER) }, onBack = onBack)
        SettingsPage.PHONE_WALLPAPER -> PhoneWallpaperRoute(onBack = onBack)
        SettingsPage.BACKUP -> BackupRoute(onBack = onBack)
        SettingsPage.ABOUT -> AboutRoute(onLicenses = { onOpen(SettingsPage.LICENSES) }, onBack = onBack)
        SettingsPage.LICENSES -> LicensesRoute(onBack = onBack)
        else -> Unit
    }
}

/** Gestures page with the Accessibility explanation for double-tap to lock (F4.5, Step 4.2). */
@Composable
private fun GesturesRoute(state: SettingsUiState, viewModel: SettingsViewModel, onOpen: (SettingsPage) -> Unit, onBack: () -> Unit) {
    var explaining by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshAccessibility()
        onPauseOrDispose {}
    }
    if (explaining) {
        PermissionRequest(
            permission = SpecialPermission.ACCESSIBILITY,
            isGranted = { viewModel.permissions.isGranted(SpecialPermission.ACCESSIBILITY) },
            openSettings = { viewModel.permissions.openSettings(SpecialPermission.ACCESSIBILITY) },
            openAppInfo = viewModel.permissions::openAppInfo,
            onResult = {
                explaining = false
                viewModel.onAccessibilityResult(it)
            }
        )
        return
    }
    GesturesScreen(
        state = state,
        actions = GesturesActions(
            onSwipeLeft = { onOpen(SettingsPage.PICK_SWIPE_LEFT) },
            onSwipeRight = { onOpen(SettingsPage.PICK_SWIPE_RIGHT) },
            onDoubleTapLock = { on ->
                if (on && !state.accessibilityEnabled) explaining = true else viewModel.setDoubleTapLock(on)
            },
            onTurnOnAccessibility = { explaining = true }
        ),
        onBack = onBack
    )
}

/** Chooses the app a side swipe opens, or "off" (F4.2, F4.3, F4.6). */
@Composable
private fun SwipeAppPicker(side: SwipeSide, state: SettingsUiState, viewModel: SettingsViewModel, onDone: () -> Unit) {
    val pick = { app: AppEntry? ->
        viewModel.setSwipeApp(side, app)
        onDone()
    }
    AppPickerScreen(
        title = stringResource(if (side == SwipeSide.LEFT) R.string.gestures_swipe_left else R.string.gestures_swipe_right),
        query = state.query,
        apps = state.pickerApps,
        onQueryChange = viewModel::onPickerQueryChange,
        onPick = pick,
        onBack = {
            viewModel.onPickerQueryChange("")
            onDone()
        },
        onNone = { pick(null) }
    )
}

/** A plain settings page: title and rows, full screen, Back handled. */
@Composable
internal fun SettingsPageScaffold(title: String, testTag: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    BackHandler(onBack = onBack)
    LazyColumn(
        Modifier.fillMaxSize().background(ZeroTheme.tokens.colors.background).safeDrawingPadding().testTag(testTag)
    ) {
        item { ScreenTitle(title) }
        content()
    }
}

/** Wireframe F1. */
@Composable
fun SettingsMainScreen(onOpen: (SettingsPage) -> Unit, onOpenDiagnostics: () -> Unit, onBack: () -> Unit) {
    SettingsPageScaffold(stringResource(R.string.settings_title), SettingsTestTags.MAIN, onBack) {
        item { NavRow(R.string.settings_home) { onOpen(SettingsPage.HOME) } }
        item { NavRow(R.string.settings_app_list) { onOpen(SettingsPage.APP_LIST) } }
        item { NavRow(R.string.settings_gestures) { onOpen(SettingsPage.GESTURES) } }
        item { NavRow(R.string.appearance_title) { onOpen(SettingsPage.APPEARANCE) } }
        item { NavRow(R.string.backup_title) { onOpen(SettingsPage.BACKUP) } }
        item { NavRow(R.string.diagnostics_title, onOpenDiagnostics) }
        item { NavRow(R.string.settings_about) { onOpen(SettingsPage.ABOUT) } }
    }
}

@Composable
internal fun NavRow(label: Int, onClick: () -> Unit) = NavRow(stringResource(label), onClick)

@Composable
internal fun NavRow(label: String, onClick: () -> Unit) = MinimalListItem(label, onClick = onClick, trailing = CHEVRON)

object SettingsTestTags {
    const val MAIN = "settings_main"
    const val HOME = "settings_home"
    const val APP_LIST = "settings_app_list"
    const val GESTURES = "settings_gestures"
    const val ABOUT = "settings_about"
    const val LICENSES = "settings_licenses"
    const val PHONE_WALLPAPER = "settings_phone_wallpaper"
    const val WALLPAPER_PREVIEW = "settings_wallpaper_preview"

    fun wallpaper(id: String) = "wallpaper_$id"

    fun wallpaperPage(id: String) = "wallpaper_page_$id"
}
