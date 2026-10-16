package com.zeroos.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.settings.AppearanceSettings
import com.zeroos.core.model.settings.TextSize
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.model.settings.Wallpaper
import com.zeroos.core.resources.R
import com.zeroos.core.ui.ClockFormat
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.SettingRow
import com.zeroos.core.ui.ToggleRow
import com.zeroos.core.ui.ZeroTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

@Composable
fun AppearanceRoute(onBack: () -> Unit, viewModel: AppearanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppearanceScreen(
        state = state,
        actions = AppearanceActions(
            onTheme = viewModel::nextTheme,
            onWallpaper = viewModel::nextWallpaper,
            onTextSize = viewModel::nextTextSize,
            onShowClock = viewModel::setShowClock,
            onShowSeconds = viewModel::setShowSeconds,
            onShowDate = viewModel::setShowDate,
            onShowStatusBar = viewModel::setShowStatusBar
        ),
        onBack = onBack
    )
}

data class AppearanceActions(
    val onTheme: () -> Unit,
    val onWallpaper: () -> Unit,
    val onTextSize: () -> Unit,
    val onShowClock: (Boolean) -> Unit,
    val onShowSeconds: (Boolean) -> Unit,
    val onShowDate: (Boolean) -> Unit,
    val onShowStatusBar: (Boolean) -> Unit
)

/** Wireframe F5: theme, wallpaper, text size, what home shows, and a live preview (F6.1–F6.5). */
@Composable
fun AppearanceScreen(state: AppearanceSettings, actions: AppearanceActions, onBack: () -> Unit, previewTime: Date = remember { Date() }) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(AppearanceTestTags.SCREEN)) {
        item { ScreenTitle(stringResource(R.string.appearance_title)) }
        item { SettingRow(stringResource(R.string.appearance_theme), stringResource(state.theme.label()), actions.onTheme) }
        item { SettingRow(stringResource(R.string.appearance_wallpaper), stringResource(state.wallpaper.label()), actions.onWallpaper) }
        item { SettingRow(stringResource(R.string.appearance_text_size), stringResource(state.textSize.label()), actions.onTextSize) }
        item { ToggleRow(stringResource(R.string.appearance_show_clock), state.showClock, actions.onShowClock) }
        if (state.showClock) {
            item { ToggleRow(stringResource(R.string.appearance_show_seconds), state.showSeconds, actions.onShowSeconds) }
        }
        item { ToggleRow(stringResource(R.string.appearance_show_date), state.showDate, actions.onShowDate) }
        item { ToggleRow(stringResource(R.string.appearance_show_status_bar), state.showStatusBar, actions.onShowStatusBar) }
        item { Preview(state, previewTime) }
    }
}

/** A small home preview drawn with the chosen theme and text size (F6). */
@Composable
private fun Preview(state: AppearanceSettings, now: Date) {
    val outer = ZeroTheme.tokens
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    // Same clock format as home: no AM/PM, so no characters Doto lacks (e.g. the narrow space before "AM").
    val format = remember(locale) { ClockFormat.forDevice(context, locale) }
    val time = remember(now) { ZonedDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault()) }
    ZeroTheme(state.theme, state.textSize) {
        val tokens = ZeroTheme.tokens
        Column(
            Modifier
                .padding(outer.spacing.screenGutter)
                .fillMaxWidth()
                .border(outer.spacing.hairline, outer.colors.divider)
                .background(tokens.colors.background)
                .padding(tokens.spacing.screenGutter)
                .testTag(AppearanceTestTags.PREVIEW)
        ) {
            if (state.showClock) {
                Text(
                    format.time(time, state.showSeconds),
                    color = tokens.colors.text,
                    fontSize = tokens.textSizes.clock
                )
            }
            if (state.showDate) {
                Text(
                    format.date(time),
                    color = tokens.colors.text,
                    fontSize = tokens.textSizes.date
                )
            }
            Text(stringResource(R.string.appearance_preview_app), color = tokens.colors.text, fontSize = tokens.textSizes.appLabel)
        }
    }
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.SYSTEM -> R.string.theme_system
}

private fun Wallpaper.label() = when (this) {
    Wallpaper.BLACK -> R.string.wallpaper_black
    Wallpaper.SYSTEM -> R.string.wallpaper_system
}

private fun TextSize.label() = when (this) {
    TextSize.SMALL -> R.string.text_size_small
    TextSize.MEDIUM -> R.string.text_size_medium
    TextSize.LARGE -> R.string.text_size_large
    TextSize.EXTRA_LARGE -> R.string.text_size_extra_large
}

object AppearanceTestTags {
    const val SCREEN = "appearance"
    const val PREVIEW = "appearance_preview"
}
