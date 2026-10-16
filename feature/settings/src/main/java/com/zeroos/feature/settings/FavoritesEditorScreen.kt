package com.zeroos.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.resources.R
import com.zeroos.core.ui.AppLabel
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.DotIcons
import com.zeroos.core.ui.DotMatrixIcon
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.SearchField
import com.zeroos.core.ui.ZeroTheme
import kotlin.math.roundToInt

/** Favorites editor with its "+ Add app" picker (Step 2.5). */
@Composable
fun FavoritesEditorRoute(onBack: () -> Unit, viewModel: FavoritesEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        AppPickerScreen(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onPick = {
                viewModel.add(it)
                picking = false
            },
            onBack = {
                viewModel.onQueryChange("")
                picking = false
            }
        )
    } else {
        FavoritesEditorScreen(state, viewModel::remove, viewModel::move, onAdd = { picking = true }, onBack = onBack)
    }
}

/**
 * Wireframe E3: favorites in order with a drag handle and "×" to remove, a count against the maximum (F3.3), and
 * "+ Add app" while there's room. Rows also offer "Move up"/"Move down" to screen readers (NFR-A1).
 */
@Composable
fun FavoritesEditorScreen(
    state: FavoritesEditorState,
    onRemove: (AppEntry) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    var dragging by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(1f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(FavoritesTestTags.EDITOR)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle(stringResource(R.string.favorites_title), Modifier.weight(1f))
                BodyText(stringResource(R.string.favorites_count, state.favorites.size, state.max), muted = true)
            }
        }
        if (state.favorites.isEmpty()) item { BodyText(stringResource(R.string.favorites_empty), muted = true) }
        itemsIndexed(state.favorites, key = { _, app -> app.key.asString() }) { index, app ->
            // While dragging, the row stays put (so drag distances are measured correctly), dims, and a line
            // shows where it will land.
            val target = dragging?.let { (it + (dragOffset / rowHeight).roundToInt()).coerceIn(0, state.favorites.lastIndex) }
            if (target == index && dragging != index) {
                val lineAbove = (dragging ?: 0) > index
                if (lineAbove) DropLine()
            }
            val moveUp = stringResource(R.string.favorites_move_up)
            val moveDown = stringResource(R.string.favorites_move_down)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.spacing.minTouchTarget)
                    .onSizeChanged { rowHeight = it.height.toFloat() }
                    .semantics {
                        customActions = listOfNotNull(
                            CustomAccessibilityAction(moveUp) { onMove(index, index - 1).let { true } }.takeIf { index > 0 },
                            CustomAccessibilityAction(moveDown) { onMove(index, index + 1).let { true } }
                                .takeIf { index < state.favorites.lastIndex }
                        )
                    }
                    .padding(horizontal = tokens.spacing.sectionGap)
            ) {
                DotMatrixIcon(
                    pattern = DotIcons.DragHandle,
                    height = tokens.spacing.sectionGap * 2,
                    contentDescription = stringResource(R.string.favorites_drag, app.label),
                    color = tokens.colors.textMuted,
                    modifier = Modifier
                        .size(tokens.spacing.minTouchTarget)
                        .padding(tokens.spacing.listItemVertical)
                        .pointerInput(index) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = index
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    val target = (index + (dragOffset / rowHeight).roundToInt()).coerceIn(0, state.favorites.lastIndex)
                                    dragging = null
                                    dragOffset = 0f
                                    onMove(index, target)
                                },
                                onDragCancel = {
                                    dragging = null
                                    dragOffset = 0f
                                }
                            ) { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                            }
                        }
                        .testTag(FavoritesTestTags.handle(index))
                )
                AppLabel(app.label, Modifier.weight(1f), muted = dragging == index)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(tokens.spacing.minTouchTarget)
                        .clickable(onClickLabel = stringResource(R.string.favorites_remove, app.label), role = Role.Button) {
                            onRemove(app)
                        }
                ) {
                    Text(text = REMOVE_SYMBOL, color = tokens.colors.textMuted, fontSize = tokens.textSizes.appLabel)
                }
            }
            if (target == index && dragging != index && (dragging ?: 0) < index) DropLine()
        }
        if (state.canAdd) item { MinimalListItem(stringResource(R.string.favorites_add), onClick = onAdd) }
    }
}

/** Where a dragged favorite will land. */
@Composable
private fun DropLine() {
    val tokens = ZeroTheme.tokens
    HorizontalDivider(
        color = tokens.colors.text,
        modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter).testTag(FavoritesTestTags.DROP_LINE)
    )
}

/** Picker for "+ Add app": searchable list of apps that aren't favorites yet. */
@Composable
fun AppPickerScreen(state: FavoritesEditorState, onQueryChange: (String) -> Unit, onPick: (AppEntry) -> Unit, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding()) {
        ScreenTitle(stringResource(R.string.favorites_pick_title))
        SearchField(state.query, autoOpenKeyboard = false, onQueryChange = onQueryChange, onGo = {
            state.candidates.firstOrNull()?.let(onPick)
        })
        HorizontalDivider(color = tokens.colors.divider, modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter))
        LazyColumn(Modifier.testTag(FavoritesTestTags.PICKER)) {
            items(state.candidates, key = {
                it.key.asString()
            }) { app -> MinimalListItem(app.label, onClick = { onPick(app) }, large = true) }
        }
    }
}

/** Doto has "×" but no "✕". */
private const val REMOVE_SYMBOL = "×"

object FavoritesTestTags {
    const val EDITOR = "favorites_editor"
    const val PICKER = "favorites_picker"
    const val DROP_LINE = "favorites_drop_line"

    fun handle(index: Int) = "favorites_handle_$index"
}
