package com.zeroos.feature.applist

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.ProfileType
import com.zeroos.core.resources.R
import com.zeroos.core.ui.AppMenu
import com.zeroos.core.ui.AppMenuActions
import com.zeroos.core.ui.AppMenuTarget
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.SearchField
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.displayName
import kotlinx.coroutines.launch

/** App list with its ViewModel (Step 2.4). Closes after an app starts. */
@Composable
fun AppListRoute(onClose: () -> Unit, viewModel: AppListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        viewModel.refreshUsage()
        onPauseOrDispose {}
    }
    fun external(target: String, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            viewModel.onExternalSearch(target)
            onClose()
        } catch (e: ActivityNotFoundException) {
            viewModel.onExternalSearch(target, e)
        }
    }
    val close = {
        viewModel.clearQuery()
        onClose()
    }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    val fullMessage = stringResource(R.string.app_menu_favorites_full, state.maxFavorites)
    menuFor?.let { app ->
        val key = app.key.asString()
        AppMenu(
            target = AppMenuTarget(app.displayName(), key in state.favoriteKeys, key in state.renamedKeys, canUninstall = app.canUninstall),
            actions = AppMenuActions(
                onToggleFavorite = { viewModel.toggleFavorite(app) { Toast.makeText(context, fullMessage, Toast.LENGTH_SHORT).show() } },
                onRename = { viewModel.rename(app, it) },
                onHide = { viewModel.hide(app) },
                onAppInfo = { viewModel.openAppInfo(app) },
                onUninstall = { viewModel.uninstall(app) }
            ),
            onDismiss = { menuFor = null }
        )
    }
    AppListScreen(
        state = state,
        onQueryChange = { if (viewModel.onQueryChange(it)) close() },
        onAppClick = { if (viewModel.launch(it)) close() },
        onAppLongClick = { menuFor = it },
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
    modifier: Modifier = Modifier,
    onAppLongClick: (AppEntry) -> Unit = {}
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
        SearchField(
            query = state.query,
            autoOpenKeyboard = state.options.keyboardAutoOpen,
            onQueryChange = onQueryChange,
            onGo = { state.results.firstOrNull()?.let(onAppClick) },
            modifier = Modifier.testTag(AppListTestTags.SEARCH)
        )
        HorizontalDivider(color = tokens.colors.divider, modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter))
        Row(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).nestedScroll(closeOnPullDown).testTag(AppListTestTags.LIST),
                contentPadding = PaddingValues(vertical = tokens.spacing.sectionGap)
            ) {
                items(state.results, key = { it.key.asString() }) { app ->
                    MinimalListItem(
                        text = labelWithTag(app),
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) },
                        large = true,
                        trailing = state.minutesLeft[app.key.packageName]?.let { timeLeft(it) }
                    )
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
                val current by remember(state.results) {
                    derivedStateOf { state.results.getOrNull(listState.firstVisibleItemIndex)?.let(::scrollerLetter) }
                }
                FastScroller(scrollerLetters(state.results), current) { letter ->
                    val index = state.results.indexOfFirst { scrollerLetter(it) == letter }
                    if (index >= 0) scope.launch { listState.scrollToItem(index) }
                }
            }
        }
    }
}

/** "12m left", "1h 5m left" or "used up" (wireframe H8). */
@Composable
private fun timeLeft(minutes: Int): String = when {
    minutes <= 0 -> stringResource(R.string.wellbeing_used_up)
    minutes < MINUTES_PER_HOUR -> stringResource(R.string.wellbeing_left_minutes, minutes)
    else -> stringResource(R.string.wellbeing_left_hours, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
}

private const val MINUTES_PER_HOUR = 60

/** "II-WhatsApp" when names are shared (D-034); work and private-space apps also get a tag, e.g. "Slack · work" (F2.10). */
@Composable
private fun labelWithTag(app: AppEntry): String {
    val name = app.displayName()
    val tag = when (app.profile) {
        ProfileType.MAIN, ProfileType.CLONE -> return name
        ProfileType.WORK -> R.string.profile_work
        ProfileType.PRIVATE -> R.string.profile_private
    }
    return stringResource(R.string.app_list_profile_tag, name, stringResource(tag))
}

/**
 * Alphabet strip (F2.7): one 48 dp-wide touch target; tap or slide along it to jump to a letter. The current
 * letter (under the finger, or else the first app on screen) is larger and brighter; while sliding, the other
 * letters fade further back.
 */
@Composable
private fun FastScroller(letters: List<Char>, current: Char?, onLetter: (Char) -> Unit) {
    if (letters.isEmpty()) return
    val tokens = ZeroTheme.tokens
    var touched by remember { mutableStateOf<Char?>(null) }
    val highlighted = touched ?: current
    // The gesture handler outlives recompositions; always call the latest callback.
    val pickLetter by rememberUpdatedState(onLetter)
    Column(
        modifier = Modifier
            .width(tokens.spacing.minTouchTarget)
            .fillMaxHeight()
            .testTag(AppListTestTags.SCROLLER)
            .slideToPick(letters) { letter ->
                if (letter != touched && letter != null) pickLetter(letter)
                touched = letter
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            ScrollerLetter(letter, isCurrent = letter == highlighted, sliding = touched != null)
        }
    }
}

/** Reports the letter under the finger while it's down, then null when it lifts. */
private fun Modifier.slideToPick(letters: List<Char>, onTouched: (Char?) -> Unit) = pointerInput(letters) {
    fun letterAt(y: Float) = letters[(y / size.height * letters.size).toInt().coerceIn(0, letters.lastIndex)]
    awaitEachGesture {
        val down = awaitFirstDown()
        onTouched(letterAt(down.position.y))
        do {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
            if (change != null && change.pressed) {
                onTouched(letterAt(change.position.y))
                change.consume()
            }
        } while (change != null && change.pressed)
        onTouched(null)
    }
}

@Composable
private fun ColumnScope.ScrollerLetter(letter: Char, isCurrent: Boolean, sliding: Boolean) {
    val tokens = ZeroTheme.tokens
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(
            letter.toString(),
            color = when {
                isCurrent -> tokens.colors.text
                sliding -> tokens.colors.textFaint
                else -> tokens.colors.textMuted
            },
            fontSize = if (isCurrent) tokens.textSizes.appLabel else tokens.textSizes.small,
            maxLines = 1,
            softWrap = false,
            // The larger letter may be taller than its slot; let it overflow instead of being cut off.
            modifier = Modifier.wrapContentHeight(unbounded = true)
        )
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
