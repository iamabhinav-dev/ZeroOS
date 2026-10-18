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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BackTitle
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.FocusMark
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.SearchField
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.displayName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * Focus picker, then the "Focus started" screen, then [onStarted] (home). Back from the picker calls [onBack], which
 * returns to wherever it was opened from (home or Settings → Focus) (F9.1, F9.2, F9.9b).
 */
@Composable
fun FocusRoute(onBack: () -> Unit, onStarted: () -> Unit, viewModel: FocusPickerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var started by rememberSaveable { mutableStateOf<Int?>(null) }
    started?.let {
        FocusStartedScreen(state.icon, it, onDone = onStarted)
        return
    }
    FocusPickerScreen(
        state,
        FocusPickerActions(
            onPreset = viewModel::selectMinutes,
            onCustom = viewModel::stepCustom,
            onSelectCustom = viewModel::selectCustom,
            onQueryChange = viewModel::onQueryChange,
            onToggle = viewModel::toggle,
            onStart = { viewModel.start { minutes -> started = minutes } }
        ),
        onBack = onBack
    )
}

data class FocusPickerActions(
    val onPreset: (Int) -> Unit,
    /** true = longer. */
    val onCustom: (Boolean) -> Unit,
    val onSelectCustom: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onToggle: (AppEntry) -> Unit,
    val onStart: () -> Unit
)

/** Wireframe H11. In landscape the icon and duration sit on the left and the app list on the right. */
@Composable
fun FocusPickerScreen(state: FocusPickerState, actions: FocusPickerActions, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    BoxWithConstraints(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(FocusTestTags.PICKER)) {
        // An outlined button keeps Start focus apart from the picked apps (D-053, user's choice).
        val start = @Composable { StartButton(actions.onStart) }
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    // Half-size icon in landscape so the durations stay in view on a short screen.
                    LazyColumn(Modifier.weight(1f)) { durationItems(state, actions, onBack, tokens.spacing.focusMarkLarge / 2) }
                    start()
                }
                LazyColumn(Modifier.weight(1f)) { appItems(state, actions) }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.weight(1f)) {
                    durationItems(state, actions, onBack, tokens.spacing.focusMarkLarge)
                    appItems(state, actions)
                }
                start()
            }
        }
    }
}

private fun LazyListScope.durationItems(state: FocusPickerState, actions: FocusPickerActions, onBack: () -> Unit, iconSize: Dp) {
    item { BackTitle(stringResource(R.string.focus_title), onBack) }
    item {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FocusMark(state.icon, iconSize, stringResource(R.string.focus_icon_description))
        }
    }
    item { BodyText(stringResource(R.string.focus_how_long)) }
    item { Presets(state.minutes, actions.onPreset) }
    item { CustomRow(state, actions.onSelectCustom, actions.onCustom) }
}

private fun LazyListScope.appItems(state: FocusPickerState, actions: FocusPickerActions) {
    item { BodyText(stringResource(R.string.focus_apps)) }
    item {
        SearchField(state.query, autoOpenKeyboard = false, onQueryChange = actions.onQueryChange, onGo = {
        }, modifier = Modifier.testTag(FocusTestTags.SEARCH))
    }
    // A thin line keeps the search apart from the app names, like the app list (D-053).
    item { Separator() }
    items(state.apps, key = { it.key.asString() }) { app -> AllowedApp(app, state.allowed, actions.onToggle) }
}

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

/** F9.9b, Appendix A: 2 s. */
private val STARTED_SCREEN = 2.seconds

object FocusTestTags {
    const val PICKER = "focus_picker"
    const val START = "focus_start"
    const val STARTED = "focus_started"
    const val END = "focus_end"
    const val REASON = "focus_end_reason"
    const val SEARCH = "focus_search"
    const val CUSTOM = "focus_custom"
}
