package com.zeroos.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.focus.FocusRepository
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.resources.R
import com.zeroos.core.ui.FocusMark
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ending focus early (F9.5, D-051). The reason is never saved or logged (F17.8). */
@HiltViewModel
class FocusEndViewModel @Inject constructor(settings: SettingsRepository, private val focus: FocusRepository) : ViewModel() {
    val icon: StateFlow<FocusIcon> = settings.settings.map { it.focus.icon }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FocusIcon.MIX)

    fun end(onEnded: () -> Unit) {
        viewModelScope.launch {
            focus.end()
            onEnded()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun FocusEndRoute(onDone: () -> Unit, viewModel: FocusEndViewModel = hiltViewModel()) {
    val icon by viewModel.icon.collectAsStateWithLifecycle()
    FocusEndScreen(icon, onEnd = { viewModel.end(onDone) }, onKeep = onDone)
}

/**
 * Wireframe H16: "End focus early?" with a "Why are you ending it?" field; "End focus" appears only once something
 * other than spaces is typed (D-051). "Keep focusing" or Back closes it. The text stays in memory only.
 */
@Composable
fun FocusEndScreen(icon: FocusIcon, onEnd: () -> Unit, onKeep: () -> Unit) {
    val tokens = ZeroTheme.tokens
    var reason by remember { mutableStateOf("") }
    BackHandler(onBack = onKeep)
    val text = @Composable { modifier: Modifier ->
        Column(modifier.verticalScroll(rememberScrollState())) {
            ScreenTitle(stringResource(R.string.focus_end_title))
            ReasonField(reason) { reason = it }
        }
    }
    val buttons = @Composable {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextAction(stringResource(R.string.focus_keep), onKeep)
            if (reason.isNotBlank()) TextAction(stringResource(R.string.focus_end), onEnd, muted = true)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(FocusTestTags.END)) {
        val mark = @Composable { FocusMark(icon, tokens.spacing.focusMarkLarge, stringResource(R.string.focus_icon_description)) }
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { mark() }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    text(Modifier.weight(1f))
                    buttons()
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(1f))
                mark()
                text(Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
                buttons()
            }
        }
    }
}

@Composable
private fun ReasonField(value: String, onChange: (String) -> Unit) {
    val tokens = ZeroTheme.tokens
    val style = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.text, fontSize = tokens.textSizes.body)
    val hint = stringResource(R.string.focus_end_reason)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(tokens.colors.text),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemVertical)
            .testTag(FocusTestTags.REASON),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) Text(hint, style = style.copy(color = tokens.colors.textMuted))
                field()
            }
        }
    )
}
