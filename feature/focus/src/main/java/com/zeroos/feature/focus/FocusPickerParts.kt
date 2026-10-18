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

@Composable
internal fun Presets(minutes: Int?, onPreset: (Int) -> Unit) {
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

/** "Start focus" as a full-width outlined button, like the selected duration (D-053). */
@Composable
internal fun StartButton(onStart: () -> Unit) {
    val tokens = ZeroTheme.tokens
    Box(
        Modifier.fillMaxWidth()
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.sectionGap)
            .border(tokens.spacing.hairline, tokens.colors.text)
            .heightIn(min = tokens.spacing.minTouchTarget)
            .clickable(role = Role.Button, onClick = onStart)
            .testTag(FocusTestTags.START),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.focus_start), color = tokens.colors.text, fontSize = tokens.textSizes.body)
    }
}

/** A thin divider line, as under the app list's search field (D-053). */
@Composable
internal fun Separator() =
    HorizontalDivider(
        color = ZeroTheme.tokens.colors.divider,
        modifier = Modifier.padding(horizontal = ZeroTheme.tokens.spacing.screenGutter)
    )

/**
 * "Custom ‹ 45 min ›": tapping the row picks the custom length, ‹ › go one step shorter or longer; outlined when
 * chosen (H11, D-051, D-053).
 */
@Composable
internal fun CustomRow(state: FocusPickerState, onSelect: () -> Unit, onStep: (Boolean) -> Unit) {
    val tokens = ZeroTheme.tokens
    val chosen = state.minutes == state.custom && state.custom !in FOCUS_PRESETS
    val shorter = stringResource(R.string.focus_shorter)
    val longer = stringResource(R.string.focus_longer)
    Row(
        Modifier.fillMaxWidth().heightIn(min = tokens.spacing.minTouchTarget).clickable(onClick = onSelect)
            .semantics { selected = chosen }.padding(start = tokens.spacing.screenGutter).testTag(FocusTestTags.CUSTOM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.focus_custom),
            color = tokens.colors.text,
            fontSize = tokens.textSizes.body,
            modifier = Modifier.weight(1f)
        )
        TextAction(
            STEP_DOWN,
            { onStep(false) },
            modifier = Modifier.semantics { contentDescription = shorter },
            compact = true,
            // No wrapping round: dimmed at the shortest length (D-054).
            enabled = state.custom > CUSTOM_STEPS.first()
        )
        Text(
            stringResource(R.string.wellbeing_minutes, state.custom),
            color = if (chosen) tokens.colors.text else tokens.colors.textMuted,
            fontSize = tokens.textSizes.body,
            modifier = Modifier.outlinedIf(chosen).padding(tokens.spacing.listItemCompact)
        )
        TextAction(
            STEP_UP,
            { onStep(true) },
            modifier = Modifier.semantics { contentDescription = longer },
            compact = true,
            enabled = state.custom < CUSTOM_STEPS.last()
        )
    }
}

/** Picked apps show their place in the list, like onboarding (Doto has no ☑, H11). */
@Composable
internal fun AllowedApp(app: AppEntry, allowed: List<String>, onToggle: (AppEntry) -> Unit) {
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
internal fun Modifier.outlinedIf(on: Boolean): Modifier =
    if (on) border(ZeroTheme.tokens.spacing.hairline, ZeroTheme.tokens.colors.text) else this

/** The ‹ › arrows on "Custom" (symbols, not words; their spoken labels are string resources). */
private const val STEP_DOWN = "‹"
private const val STEP_UP = "›"
