package com.zeroos.feature.applist

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.ProfileType
import com.zeroos.core.resources.R
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ZeroTheme
import kotlinx.coroutines.launch

/** App list with its ViewModel (Step 2.4). Closes after an app starts. */
@Composable
fun AppListRoute(onClose: () -> Unit, viewModel: AppListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun external(target: String, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            viewModel.onExternalSearch(target)
            onClose()
        } catch (e: ActivityNotFoundException) {
            viewModel.onExternalSearchFailed(target, e)
        }
    }
    val close = {
        viewModel.clearQuery()
        onClose()
    }
    AppListScreen(
        state = state,
        onQueryChange = { if (viewModel.onQueryChange(it)) close() },
        onAppClick = { if (viewModel.launch(it)) close() },
        onSearchWeb = { external("web", Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, it)) },
        onSearchPlayStore = { external("play store", Intent(Intent.ACTION_VIEW, "market://search?q=${Uri.encode(it)}".toUri())) },
        onClose = close
    )
}

/**
 * Search field and alphabetical list (F2, wireframes B1–B4). Back or swiping down at the top of the list
 * closes it (F2.11).
 */
@Composable
fun AppListScreen(
    state: AppListUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppEntry) -> Unit,
    onSearchWeb: (String) -> Unit,
    onSearchPlayStore: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ZeroTheme.tokens
    val listState = rememberLazyListState()
    val closeOnPullDown = rememberPullDownToClose(onClose)
    BackHandler(onBack = onClose)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .safeDrawingPadding()
            .imePadding()
    ) {
        SearchField(state.query, state.options.keyboardAutoOpen, onQueryChange, onGo = { state.results.firstOrNull()?.let(onAppClick) })
        HorizontalDivider(color = tokens.colors.divider, modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter))
        Row(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).nestedScroll(closeOnPullDown).testTag(AppListTestTags.LIST),
                contentPadding = PaddingValues(vertical = tokens.spacing.sectionGap)
            ) {
                items(state.results, key = { it.key.asString() }) { app ->
                    MinimalListItem(text = labelWithTag(app), onClick = { onAppClick(app) }, large = true)
                }
                if (state.showNoMatch) {
                    item { MinimalListItem(stringResource(R.string.app_list_search_web, state.query), { onSearchWeb(state.query) }) }
                    item {
                        MinimalListItem(stringResource(R.string.app_list_search_play_store, state.query), {
                            onSearchPlayStore(state.query)
                        })
                    }
                }
            }
            if (state.options.fastScroller && state.query.isBlank()) {
                val scope = rememberCoroutineScope()
                FastScroller(scrollerLetters(state.results)) { letter ->
                    val index = state.results.indexOfFirst { AppSearchIndex.normalize(it.label).firstOrNull()?.uppercaseChar() == letter }
                    if (index >= 0) scope.launch { listState.scrollToItem(index) }
                }
            }
        }
    }
}

/** "WhatsApp · clone" for apps outside the main profile (F2.10). */
@Composable
private fun labelWithTag(app: AppEntry): String {
    val tag = when (app.profile) {
        ProfileType.MAIN -> return app.label
        ProfileType.CLONE -> R.string.profile_clone
        ProfileType.WORK -> R.string.profile_work
        ProfileType.PRIVATE -> R.string.profile_private
    }
    return stringResource(R.string.app_list_profile_tag, app.label, stringResource(tag))
}

@Composable
private fun SearchField(query: String, autoOpenKeyboard: Boolean, onQueryChange: (String) -> Unit, onGo: () -> Unit) {
    val tokens = ZeroTheme.tokens
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoOpenKeyboard) {
        if (autoOpenKeyboard) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    val style = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.text, fontSize = tokens.textSizes.appLabel)
    val label = stringResource(R.string.app_list_search_label)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(tokens.colors.text),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .semantics { contentDescription = label }
            .testTag(AppListTestTags.SEARCH)
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemVertical),
        decorationBox = { field ->
            Box {
                if (query.isEmpty()) {
                    Text(
                        stringResource(R.string.app_list_search_hint),
                        style = style.copy(color = tokens.colors.textMuted)
                    )
                }
                field()
            }
        }
    )
}

/**
 * Alphabet strip (F2.7): one 48 dp-wide touch target; tap or drag along it to jump to a letter.
 */
@Composable
private fun FastScroller(letters: List<Char>, onLetter: (Char) -> Unit) {
    if (letters.isEmpty()) return
    val tokens = ZeroTheme.tokens
    fun letterAt(y: Float, height: Int) = letters[(y / height * letters.size).toInt().coerceIn(0, letters.lastIndex)]
    Column(
        modifier = Modifier
            .width(tokens.spacing.minTouchTarget)
            .fillMaxHeight()
            .testTag(AppListTestTags.SCROLLER)
            .pointerInput(letters) { detectTapGestures { onLetter(letterAt(it.y, size.height)) } }
            .pointerInput(letters) { detectVerticalDragGestures { change, _ -> onLetter(letterAt(change.position.y, size.height)) } },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach {
            Text(it.toString(), color = tokens.colors.textMuted, fontSize = tokens.textSizes.small, modifier = Modifier.weight(1f))
        }
    }
}

/** Closes the list when it's pulled down past the swipe distance while already at the top (F2.11). */
@Composable
private fun rememberPullDownToClose(onClose: () -> Unit): NestedScrollConnection {
    val threshold = with(LocalDensity.current) { ZeroTheme.tokens.gestures.swipeDistance.toPx() }
    return remember(threshold, onClose) {
        object : NestedScrollConnection {
            private var pulled = 0f
            private var closed = false

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                pulled = if (available.y > 0f && source == NestedScrollSource.UserInput) pulled + available.y else 0f
                if (pulled > threshold && !closed) {
                    closed = true
                    onClose()
                }
                return Offset.Zero
            }

            /** A new gesture may close again. */
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pulled = 0f
                closed = false
                return Velocity.Zero
            }
        }
    }
}

object AppListTestTags {
    const val LIST = "app_list"
    const val SEARCH = "app_list_search"
    const val SCROLLER = "app_list_scroller"
}
