package com.zeroos.feature.wellbeing

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.data.wellbeing.EXTRA_MINUTES
import com.zeroos.core.data.wellbeing.StopScreen
import com.zeroos.core.model.AppEntry
import com.zeroos.core.resources.R
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.displayName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Shows the stop screen instead of [content] while an app waits behind one (F8.1, F8.4). Back and the Home button
 * count as "Go back".
 */
@Composable
fun StopHost(homePresses: Flow<Unit>, viewModel: StopViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val puns = PunCounts(
        stringArrayResource(R.array.wellbeing_puns_pause).size,
        stringArrayResource(R.array.wellbeing_puns_limit).size
    )
    LaunchedEffect(homePresses) { homePresses.collect { viewModel.goBack() } }
    val app = pending
    if (app == null) {
        content()
        return
    }
    LaunchedEffect(app) { viewModel.check(app, puns) }
    BackHandler(onBack = viewModel::goBack)
    StopScreen(
        state.takeIf { it.app == app } ?: StopUiState(app),
        StopActions(onOpen = viewModel::openAnyway, onMoreMinutes = viewModel::moreMinutes, onBack = viewModel::goBack)
    )
}

data class StopActions(val onOpen: () -> Unit, val onMoreMinutes: () -> Unit, val onBack: () -> Unit)

/** Wireframes H6 and H7; blank while today's usage is being read. */
@Composable
fun StopScreen(state: StopUiState, actions: StopActions) {
    val tokens = ZeroTheme.tokens
    Box(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(StopTestTags.ROOT)) {
        val app = state.app ?: return@Box
        key(app.key) {
            when (val screen = state.screen) {
                is StopScreen.Pause -> PauseScreen(app, screen, state.pun, actions)
                is StopScreen.Limit -> LimitScreen(app, screen, state.pun, actions)
                null -> Unit
            }
        }
    }
}

/** Wireframe H6 (F8.1, F8.2, F8.13, F8.15). The intention is kept only in memory: never saved or logged (F17.8). */
@Composable
private fun PauseScreen(app: AppEntry, screen: StopScreen.Pause, pun: Int?, actions: StopActions) {
    var left by rememberSaveable { mutableIntStateOf(screen.seconds) }
    var intention by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1.seconds)
            left--
        }
    }
    StopLayout(
        art = { BreathingDots(screen.seconds, still = rememberReducedMotion()) },
        buttons = {
            TextAction(stringResource(R.string.wellbeing_go_back), actions.onBack, muted = true)
            // With the prompt on, an intention must be typed first (D-043); spaces don't count.
            if (left == 0 &&
                (!screen.intentionPrompt || intention.isNotBlank())
            ) {
                TextAction(stringResource(R.string.wellbeing_open_app, app.displayName()), actions.onOpen)
            }
        }
    ) {
        Line(pun?.let { stringArrayResource(R.array.wellbeing_puns_pause).getOrNull(it) } ?: stringResource(R.string.wellbeing_pause_plain))
        Text(
            if (left > 0) left.toString() else "",
            color = ZeroTheme.tokens.colors.text,
            fontSize = ZeroTheme.tokens.textSizes.title,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag(StopTestTags.COUNTDOWN)
        )
        if (screen.intentionPrompt) IntentionField(intention) { intention = it }
    }
}

/** Wireframe H7 (F8.4, F8.13, F8.15). */
@Composable
private fun LimitScreen(app: AppEntry, screen: StopScreen.Limit, pun: Int?, actions: StopActions) {
    StopLayout(
        art = { DrainingBattery(still = rememberReducedMotion()) },
        buttons = {
            TextAction(stringResource(R.string.wellbeing_go_home), actions.onBack)
            if (screen.extrasLeft > 0) {
                TextAction(
                    pluralStringResource(R.plurals.wellbeing_more_minutes, EXTRA_MINUTES, EXTRA_MINUTES, screen.extrasLeft),
                    actions.onMoreMinutes,
                    muted = true
                )
            }
        }
    ) {
        Line(pun?.let { stringArrayResource(R.array.wellbeing_puns_limit).getOrNull(it) } ?: stringResource(R.string.wellbeing_limit_plain))
        Text(
            stringResource(
                R.string.wellbeing_used_today,
                app.displayName(),
                stringResource(R.string.wellbeing_minutes, screen.usedMinutes)
            ),
            color = ZeroTheme.tokens.colors.textMuted,
            fontSize = ZeroTheme.tokens.textSizes.body,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StopLayout(art: @Composable () -> Unit, buttons: @Composable () -> Unit, text: @Composable ColumnScope.() -> Unit) {
    val tokens = ZeroTheme.tokens
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        art()
        Column(
            Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
            content = text
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { buttons() }
    }
}

@Composable
internal fun Line(text: String) = Text(
    text,
    color = ZeroTheme.tokens.colors.text,
    fontSize = ZeroTheme.tokens.textSizes.appLabel,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth()
)

/** "Why are you opening it?" (F8.2). */
@Composable
private fun IntentionField(value: String, onChange: (String) -> Unit) {
    val tokens = ZeroTheme.tokens
    val style = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.text, fontSize = tokens.textSizes.body)
    val hint = stringResource(R.string.wellbeing_intention_hint)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(tokens.colors.text),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
            .padding(vertical = tokens.spacing.listItemVertical).testTag(StopTestTags.INTENTION),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) Text(hint, style = style.copy(color = tokens.colors.textMuted))
                field()
            }
        }
    )
}

/** System "Remove animations" is on (NFR-A4): the dot art stays still. */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

object StopTestTags {
    const val ROOT = "stop_screen"
    const val COUNTDOWN = "stop_countdown"
    const val INTENTION = "stop_intention"
    const val BREAK = "break_screen"
    const val BREAK_BUTTON = "break_button"
}
