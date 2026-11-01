package com.zeroos.feature.wellbeing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BackTitle
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.DotMatrixIcon
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.durationText
import kotlinx.coroutines.flow.Flow

/** Shows the screen-time lock over everything while an app waits behind it (F19.3), otherwise [content]. */
@Composable
fun LockHost(homePresses: Flow<Unit>, viewModel: LockViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val app by viewModel.pending.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    LaunchedEffect(homePresses) { homePresses.collect { viewModel.goHome() } }
    val waiting = app
    if (waiting == null) {
        content()
        return
    }
    val puns = LockPunCounts(stringArrayResource(R.array.lock_puns).size, stringArrayResource(R.array.lock_wrong_puns).size)
    LaunchedEffect(waiting) { viewModel.show(puns) }
    BackHandler(onBack = viewModel::back)
    when (state.page) {
        LockPage.LOCKED -> LockScreen(state.usedMillis, state.pun, onGoHome = viewModel::goHome, onUnlock = viewModel::openUnlock)
        LockPage.UNLOCK -> UnlockScreen(state.phrase, onSubmit = { viewModel.submit(it, puns) }, onBack = viewModel::back)
        LockPage.WRONG -> WrongPhraseScreen(state.wrongPun, onGoHome = viewModel::goHome, onTryAgain = viewModel::tryAgain)
    }
}

/** Wireframe L3: today's cap is reached; also drawn over other apps by the Accessibility service. */
@Composable
fun LockScreen(usedMillis: Long, pun: Int, onGoHome: () -> Unit, onUnlock: () -> Unit) {
    LockBackground(LockTestTags.LOCKED) {
        StopLayout(
            art = { Padlock() },
            buttons = {
                TextAction(stringResource(R.string.wellbeing_go_home), onGoHome)
                TextAction(stringResource(R.string.lock_unlock), onUnlock, muted = true)
            }
        ) {
            Line(stringResource(R.string.lock_title))
            Muted(stringResource(R.string.lock_used, durationText(usedMillis)))
            Line(punLine(R.array.lock_puns, pun))
        }
    }
}

/** Wireframe L4: type the phrase shown above the field to unlock for 15 minutes (F19.4). */
@Composable
fun UnlockScreen(phrase: String, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    var typed by remember { mutableStateOf("") }
    LockBackground(LockTestTags.UNLOCK) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            BackTitle(stringResource(R.string.lock_unlock), onBack)
            BodyText(stringResource(R.string.lock_type_phrase, phrase))
            PhraseField(typed, onChange = { typed = it }, onDone = { if (typed.isNotEmpty()) onSubmit(typed) })
            Row(Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.sectionGap), horizontalArrangement = Arrangement.End) {
                TextAction(stringResource(R.string.lock_unlock), { onSubmit(typed) }, enabled = typed.isNotEmpty())
            }
        }
    }
}

/** Wireframe L5: a wrong phrase gets an "I am ___, I need ___" pun and a question (F19.5). */
@Composable
fun WrongPhraseScreen(pun: Int, onGoHome: () -> Unit, onTryAgain: () -> Unit) {
    LockBackground(LockTestTags.WRONG) {
        StopLayout(
            art = { Padlock() },
            buttons = {
                TextAction(stringResource(R.string.wellbeing_go_home), onGoHome)
                TextAction(stringResource(R.string.lock_try_again), onTryAgain, muted = true)
            }
        ) {
            Line(punLine(R.array.lock_wrong_puns, pun))
            Line(stringResource(R.string.lock_really_need))
        }
    }
}

@Composable
private fun LockBackground(tag: String, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(ZeroTheme.tokens.colors.background).safeDrawingPadding().testTag(tag)) { content() }
}

@Composable
private fun Muted(text: String) = Text(
    text,
    color = ZeroTheme.tokens.colors.textMuted,
    fontSize = ZeroTheme.tokens.textSizes.body,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth()
)

/** No autocorrect or suggestions: the phrase has to be typed as it is (F19.4). Never saved or logged. */
@Composable
private fun PhraseField(value: String, onChange: (String) -> Unit, onDone: () -> Unit) {
    val tokens = ZeroTheme.tokens
    val style = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.text, fontSize = tokens.textSizes.body)
    val label = stringResource(R.string.lock_phrase_field)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(tokens.colors.text),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemVertical)
            .semantics { contentDescription = label }
            .testTag(LockTestTags.FIELD),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) Text(label, style = style.copy(color = tokens.colors.textMuted))
                field()
            }
        }
    )
}

/** A dotted padlock, drawn like the other stop-screen art (Doto has no icon glyphs). */
@Composable
private fun Padlock() {
    DotMatrixIcon(PADLOCK, height = ZeroTheme.tokens.spacing.stopArtWidth, contentDescription = "")
}

private val PADLOCK = listOf(
    "  ●●●●●  ",
    " ●     ● ",
    " ●     ● ",
    " ●     ● ",
    "●●●●●●●●●",
    "●●●●●●●●●",
    "●●●● ●●●●",
    "●●●● ●●●●",
    "●●●●●●●●●",
    "●●●●●●●●●"
)

object LockTestTags {
    const val LOCKED = "lock_screen"
    const val UNLOCK = "lock_unlock"
    const val WRONG = "lock_wrong"
    const val FIELD = "lock_phrase_field"
}
