package com.zeroos.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.docs.DocumentsGate
import com.zeroos.core.data.docs.DocumentsRepository
import com.zeroos.core.data.docs.ReleaseDocument
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.DocumentReader
import com.zeroos.core.ui.DotMatrixIcon
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReadBeforeUsingState(
    val versionName: String = "",
    val accepted: Boolean = false,
    /** The document open in the reader (K3), or null on the page itself (K2). */
    val open: ReleaseDocument? = null,
    val openText: String = ""
)

/**
 * F14.8: the release's README notes, LICENSE and notices must be read and accepted before ZeroOS is used, on first
 * start and again once after a release that changed them (wireframes K2, K3). Not skippable.
 */
@HiltViewModel
class ReadBeforeUsingViewModel @Inject constructor(
    private val documents: DocumentsRepository,
    private val logger: Logger,
    private val savedState: SavedStateHandle
) : ViewModel() {

    /** Whether the page must come first; LOADING until the documents and settings are read. */
    val gate: StateFlow<DocumentsGate> = documents.gate.stateIn(viewModelScope, SharingStarted.Eagerly, DocumentsGate.LOADING)

    private val mutableState = MutableStateFlow(
        ReadBeforeUsingState(documents.versionName, accepted = savedState[KEY_ACCEPTED] ?: false)
    )
    val state: StateFlow<ReadBeforeUsingState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { documents.load() }
        savedState.get<String>(KEY_OPEN)?.let { name -> ReleaseDocument.entries.firstOrNull { it.name == name } }?.let(::open)
    }

    fun onShown(gate: DocumentsGate) = logger.info(TAG, "documents page shown (${gate.name.lowercase()})")

    fun setAccepted(accepted: Boolean) {
        savedState[KEY_ACCEPTED] = accepted
        mutableState.value = mutableState.value.copy(accepted = accepted)
    }

    fun open(document: ReleaseDocument) {
        savedState[KEY_OPEN] = document.name
        logger.info(TAG, "opened ${document.name.lowercase()}")
        viewModelScope.launch {
            val text = documents.text(document)
            mutableState.value = mutableState.value.copy(open = document, openText = text)
        }
    }

    fun close() {
        savedState.remove<String>(KEY_OPEN)
        mutableState.value = mutableState.value.copy(open = null, openText = "")
    }

    fun proceed() {
        if (!state.value.accepted) return
        viewModelScope.launch { documents.accept() }
    }

    private companion object {
        const val TAG = "documents"
        const val KEY_ACCEPTED = "documents_accepted"
        const val KEY_OPEN = "documents_open"
    }
}

data class ReadBeforeUsingActions(
    val onToggle: (Boolean) -> Unit,
    val onOpen: (ReleaseDocument) -> Unit,
    val onClose: () -> Unit,
    val onContinue: () -> Unit
)

@Composable
fun ReadBeforeUsingRoute(gate: DocumentsGate, viewModel: ReadBeforeUsingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(gate) { viewModel.onShown(gate) }
    ReadBeforeUsingScreen(
        changed = gate == DocumentsGate.CHANGED,
        state = state,
        actions = ReadBeforeUsingActions(viewModel::setAccepted, viewModel::open, viewModel::close, viewModel::proceed)
    )
}

/** Wireframe K2, or K3 while a document is open. Two panes when wider than tall. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ReadBeforeUsingScreen(changed: Boolean, state: ReadBeforeUsingState, actions: ReadBeforeUsingActions) {
    val open = state.open
    if (open != null) {
        DocumentReader(stringResource(open.title()), state.openText, onBack = actions.onClose)
        return
    }
    val tokens = ZeroTheme.tokens
    val size = LocalWindowInfo.current.containerSize
    val root = Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(ReadBeforeUsingTags.ROOT)
    if (size.width > size.height) {
        Row(root) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { Notice(changed, state, actions) }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                Agreement(state, actions)
            }
        }
    } else {
        Column(root.verticalScroll(rememberScrollState())) {
            Notice(changed, state, actions)
            Agreement(state, actions)
        }
    }
}

@Composable
private fun Notice(changed: Boolean, state: ReadBeforeUsingState, actions: ReadBeforeUsingActions) {
    ScreenTitle(stringResource(if (changed) R.string.docs_gate_title_changed else R.string.docs_gate_title))
    BodyText(stringResource(R.string.docs_gate_body))
    BodyText(stringResource(R.string.docs_gate_read_these, state.versionName), muted = true)
    ReleaseDocument.entries.forEach { document ->
        MinimalListItem(stringResource(document.title()), onClick = { actions.onOpen(document) }, trailing = CHEVRON)
    }
}

@Composable
private fun Agreement(state: ReadBeforeUsingState, actions: ReadBeforeUsingActions) {
    val tokens = ZeroTheme.tokens
    Row(
        Modifier.fillMaxWidth()
            .clickable { actions.onToggle(!state.accepted) }
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                toggleableState = ToggleableState(state.accepted)
            }
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemVertical)
            .testTag(ReadBeforeUsingTags.ACCEPT),
        verticalAlignment = Alignment.Top
    ) {
        // Doto has no ☐/☑: a dotted box, filled in the middle when ticked (like the website).
        DotMatrixIcon(
            if (state.accepted) BOX_TICKED else BOX_EMPTY,
            height = tokens.spacing.sectionGap * BOX_SCALE,
            contentDescription = "",
            modifier = Modifier.padding(top = tokens.spacing.listItemCompact, end = tokens.spacing.listItemVertical)
        )
        Text(stringResource(R.string.docs_gate_accept), color = tokens.colors.text, fontSize = tokens.textSizes.body)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextAction(stringResource(R.string.common_continue), actions.onContinue, enabled = state.accepted)
    }
}

private fun ReleaseDocument.title(): Int = when (this) {
    ReleaseDocument.README -> R.string.docs_readme
    ReleaseDocument.LICENSE -> R.string.docs_license
    ReleaseDocument.NOTICES -> R.string.docs_notices
}

private const val CHEVRON = "›"
private const val BOX_SCALE = 3
private val BOX_EMPTY = listOf("●●●●●", "●   ●", "●   ●", "●   ●", "●●●●●")
private val BOX_TICKED = listOf("●●●●●", "●   ●", "● ● ●", "●   ●", "●●●●●")

object ReadBeforeUsingTags {
    const val ROOT = "read_before_using"
    const val ACCEPT = "read_before_using_accept"
}
