package com.zeroos.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.FocusMark
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.SettingRow
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.displayName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/** Focus picker, then the "Focus started" screen, then [onClose] back to home (F9.1, F9.2, F9.9b). */
@Composable
fun FocusRoute(onClose: () -> Unit, viewModel: FocusPickerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var started by rememberSaveable { mutableStateOf<Int?>(null) }
    started?.let {
        FocusStartedScreen(state.icon, it, onDone = onClose)
        return
    }
    FocusPickerScreen(
        state,
        FocusPickerActions(
            onPreset = viewModel::selectMinutes,
            onCustom = viewModel::cycleCustom,
            onToggle = viewModel::toggle,
            onStart = { viewModel.start { minutes -> started = minutes } }
        ),
        onBack = onClose
    )
}

data class FocusPickerActions(
    val onPreset: (Int) -> Unit,
    val onCustom: () -> Unit,
    val onToggle: (AppEntry) -> Unit,
    val onStart: () -> Unit
)

/** Wireframe H11. In landscape the icon and duration sit on the left and the app list on the right. */
@Composable
fun FocusPickerScreen(state: FocusPickerState, actions: FocusPickerActions, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    BoxWithConstraints(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(FocusTestTags.PICKER)) {
        val start = @Composable {
            TextAction(stringResource(R.string.focus_start), actions.onStart, modifier = Modifier.testTag(FocusTestTags.START))
        }
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    LazyColumn(Modifier.weight(1f)) { durationItems(state, actions) }
                    start()
                }
                LazyColumn(Modifier.weight(1f)) { appItems(state, actions) }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.weight(1f)) {
                    durationItems(state, actions)
                    appItems(state, actions)
                }
                start()
            }
        }
    }
}

private fun LazyListScope.durationItems(state: FocusPickerState, actions: FocusPickerActions) {
    item { ScreenTitle(stringResource(R.string.focus_title)) }
    item {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FocusMark(state.icon, ZeroTheme.tokens.spacing.focusMarkLarge, stringResource(R.string.focus_icon_description))
        }
    }
    item { BodyText(stringResource(R.string.focus_how_long)) }
    item { Presets(state.minutes, actions.onPreset) }
    item {
        val custom = state.minutes?.takeIf { it !in FOCUS_PRESETS }
        SettingRow(
            stringResource(R.string.focus_custom),
            stringResource(R.string.wellbeing_minutes, custom ?: CUSTOM_HINT),
            actions.onCustom,
            Modifier.semantics { selected = custom != null }.outlinedIf(custom != null)
        )
    }
}

private fun LazyListScope.appItems(state: FocusPickerState, actions: FocusPickerActions) {
    item { BodyText(stringResource(R.string.focus_apps)) }
    items(state.apps, key = { it.key.asString() }) { app -> AllowedApp(app, state.allowed, actions.onToggle) }
}

@Composable
private fun Presets(minutes: Int?, onPreset: (Int) -> Unit) {
    val tokens = ZeroTheme.tokens
    Row(
        Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
    ) {
        FOCUS_PRESETS.forEach { preset ->
            TextAction(
                stringResource(R.string.wellbeing_minutes, preset),
                { onPreset(preset) },
                modifier = Modifier.semantics { selected = minutes == preset }.outlinedIf(minutes == preset),
                compact = true
            )
        }
    }
}

/** Picked apps show their place in the list, like onboarding (Doto has no ☑, H11). */
@Composable
private fun AllowedApp(app: AppEntry, allowed: List<String>, onToggle: (AppEntry) -> Unit) {
    val position = allowed.indexOf(app.key.asString())
    val checked = position >= 0
    MinimalListItem(
        text = app.displayName(),
        onClick = { onToggle(app) },
        muted = !checked,
        trailing = if (checked) (position + 1).toString() else null,
        large = true,
        modifier = Modifier.semantics {
            role = Role.Checkbox
            toggleableState = ToggleableState(checked)
        }
    )
}

@Composable
private fun Modifier.outlinedIf(on: Boolean): Modifier =
    if (on) border(ZeroTheme.tokens.spacing.hairline, ZeroTheme.tokens.colors.text) else this

/** Wireframe H15 (F9.9b): the icon and "Focus · 50 min" for 2 s; a tap skips it. */
@Composable
fun FocusStartedScreen(icon: FocusIcon, minutes: Int, onDone: () -> Unit) {
    val tokens = ZeroTheme.tokens
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        delay(STARTED_SCREEN)
        done()
    }
    BackHandler(onBack = onDone)
    Column(
        Modifier.fillMaxSize().background(tokens.colors.background).clickable(onClick = onDone).testTag(FocusTestTags.STARTED),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FocusMark(icon, tokens.spacing.focusMarkLarge, stringResource(R.string.focus_icon_description))
        Text(
            stringResource(R.string.focus_started, minutes),
            color = tokens.colors.text,
            fontSize = tokens.textSizes.title,
            modifier = Modifier.padding(tokens.spacing.sectionGap)
        )
    }
}

/** Shown on "Custom" until a custom length is chosen (H11 shows ‹45 min›). */
private const val CUSTOM_HINT = 45

/** F9.9b, Appendix A: 2 s. */
private val STARTED_SCREEN = 2.seconds

object FocusTestTags {
    const val PICKER = "focus_picker"
    const val START = "focus_start"
    const val STARTED = "focus_started"
}
