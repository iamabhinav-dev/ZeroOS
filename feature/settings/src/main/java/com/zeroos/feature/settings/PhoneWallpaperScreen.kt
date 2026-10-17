package com.zeroos.feature.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.data.wallpaper.WallpaperItem
import com.zeroos.core.data.wallpaper.WallpaperTone
import com.zeroos.core.model.settings.WallpaperScreen
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme

/** Settings → Appearance → Phone wallpaper, with its ViewModel (F6.8). */
@Composable
fun PhoneWallpaperRoute(onBack: () -> Unit, viewModel: PhoneWallpaperViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dark = ZeroTheme.tokens.isDark
    // The gallery shows only wallpapers of the theme on screen (D-038).
    LaunchedEffect(dark) { viewModel.setTone(if (dark) WallpaperTone.DARK else WallpaperTone.LIGHT) }
    PhoneWallpaperScreen(
        state = state,
        actions = PhoneWallpaperActions(
            onSelect = viewModel::select,
            onChooseScreen = viewModel::chooseScreen,
            onConfirm = viewModel::confirm,
            onCancelAsk = viewModel::cancelAsk,
            onRestore = viewModel::restorePrevious
        ),
        thumbnail = { item, width, height -> viewModel.thumbnail(item, width, height).asImageBitmap() },
        onBack = onBack
    )
}

data class PhoneWallpaperActions(
    val onSelect: (WallpaperItem) -> Unit,
    val onChooseScreen: (WallpaperScreen) -> Unit,
    /** true = both screens, false = only the chosen one. */
    val onConfirm: (Boolean) -> Unit,
    val onCancelAsk: () -> Unit,
    val onRestore: (WallpaperScreen) -> Unit
)

/** Wireframe F5b. [thumbnail] makes a preview of the given pixel size (off the main thread in the app). */
@Composable
fun PhoneWallpaperScreen(
    state: PhoneWallpaperState,
    actions: PhoneWallpaperActions,
    thumbnail: suspend (WallpaperItem, Int, Int) -> ImageBitmap?,
    onBack: () -> Unit
) {
    SettingsPageScaffold(stringResource(R.string.phone_wallpaper_title), SettingsTestTags.PHONE_WALLPAPER, onBack) {
        state.items.chunked(GALLERY_COLUMNS).forEach { row ->
            item(key = row.first().id) { GalleryRow(row, state.selected?.id, thumbnail, actions.onSelect) }
        }
        item { BodyText(status(state), muted = true, small = true) }
        item { NavRow(R.string.phone_wallpaper_set_home) { actions.onChooseScreen(WallpaperScreen.HOME) } }
        item { NavRow(R.string.phone_wallpaper_set_lock) { actions.onChooseScreen(WallpaperScreen.LOCK) } }
        state.result?.let { outcome -> outcomeLines(outcome).forEach { line -> item { BodyText(stringResource(line)) } } }
        if (state.history.previous(WallpaperScreen.HOME) != null) {
            item { NavRow(R.string.phone_wallpaper_restore_home) { actions.onRestore(WallpaperScreen.HOME) } }
        }
        if (state.history.previous(WallpaperScreen.LOCK) != null) {
            item { NavRow(R.string.phone_wallpaper_restore_lock) { actions.onRestore(WallpaperScreen.LOCK) } }
        }
        item { BodyText(stringResource(R.string.phone_wallpaper_note), muted = true, small = true) }
    }
    state.asking?.let { AskOtherScreen(it, actions) }
}

@Composable
private fun GalleryRow(
    row: List<WallpaperItem>,
    selectedId: String?,
    thumbnail: suspend (WallpaperItem, Int, Int) -> ImageBitmap?,
    onSelect: (WallpaperItem) -> Unit
) {
    val tokens = ZeroTheme.tokens
    Row(
        Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.sectionGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.screenGutter)
    ) {
        row.forEach { GalleryItem(it, it.id == selectedId, thumbnail, onSelect, Modifier.weight(1f)) }
        repeat(GALLERY_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun GalleryItem(
    item: WallpaperItem,
    selected: Boolean,
    thumbnail: suspend (WallpaperItem, Int, Int) -> ImageBitmap?,
    onSelect: (WallpaperItem) -> Unit,
    modifier: Modifier
) {
    val tokens = ZeroTheme.tokens
    val label = wallpaperLabel(item.name)
    val density = LocalDensity.current
    val width = with(density) { tokens.spacing.thumbnailWidth.roundToPx() }
    val image by produceState<ImageBitmap?>(null, item.id) { value = thumbnail(item, width, width * THUMB_HEIGHT / THUMB_WIDTH) }
    Column(
        modifier
            .clickable(onClickLabel = label, role = Role.RadioButton) { onSelect(item) }
            .semantics { this.selected = selected }
            .testTag(SettingsTestTags.wallpaper(item.id))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(THUMB_WIDTH.toFloat() / THUMB_HEIGHT)
                .border(
                    if (selected) tokens.spacing.selectedOutline else tokens.spacing.hairline,
                    if (selected) tokens.colors.text else tokens.colors.divider
                )
                .background(tokens.colors.background)
        ) {
            image?.let {
                Image(
                    it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(THUMB_WIDTH.toFloat() / THUMB_HEIGHT)
                )
            }
        }
        Text(label, color = if (selected) tokens.colors.text else tokens.colors.textMuted, fontSize = tokens.textSizes.small, maxLines = 1)
    }
}

@Composable
private fun AskOtherScreen(chosen: WallpaperScreen, actions: PhoneWallpaperActions) {
    val home = chosen == WallpaperScreen.HOME
    AlertDialog(
        onDismissRequest = actions.onCancelAsk,
        containerColor = ZeroTheme.tokens.colors.background,
        title = { Text(stringResource(if (home) R.string.phone_wallpaper_ask_lock else R.string.phone_wallpaper_ask_home)) },
        confirmButton = {
            TextAction(stringResource(R.string.phone_wallpaper_both), onClick = { actions.onConfirm(true) }, compact = true)
        },
        dismissButton = {
            TextAction(
                stringResource(if (home) R.string.phone_wallpaper_only_home else R.string.phone_wallpaper_only_lock),
                onClick = { actions.onConfirm(false) },
                compact = true
            )
        }
    )
}

@Composable
private fun status(state: PhoneWallpaperState): String {
    val none = stringResource(R.string.phone_wallpaper_not_set)
    val home = state.history.current(WallpaperScreen.HOME)?.let { wallpaperLabel(nameOf(it)) } ?: none
    val lock = state.history.current(WallpaperScreen.LOCK)?.let { wallpaperLabel(nameOf(it)) } ?: none
    return stringResource(R.string.phone_wallpaper_status, home, lock)
}

/** "rose_dark" → "rose"; the solids keep their id. */
private fun nameOf(id: String) = id.removeSuffix("_dark").removeSuffix("_light")

/** Result lines for the last action (wireframe F5b). */
internal fun outcomeLines(outcome: WallpaperOutcome): List<Int> = when (outcome) {
    WallpaperOutcome.Gone -> listOf(R.string.phone_wallpaper_gone)

    is WallpaperOutcome.Set -> buildList {
        val result = outcome.result
        if (result.failed) {
            add(R.string.phone_wallpaper_failed)
            return@buildList
        }
        val home = WallpaperScreen.HOME in result.accepted
        val lock = WallpaperScreen.LOCK in result.accepted
        when {
            home && lock -> add(R.string.phone_wallpaper_done_both)
            home -> add(R.string.phone_wallpaper_done_home)
            lock -> add(R.string.phone_wallpaper_done_lock)
        }
        if (WallpaperScreen.LOCK in result.rejected) add(R.string.phone_wallpaper_lock_rejected)
        // Some phones (e.g. the phone's Themes app) keep their own lock screen even after accepting one (D-038).
        if (lock) add(R.string.phone_wallpaper_lock_hint)
        if (result.homeBackgroundSwitched) add(R.string.phone_wallpaper_switched)
    }
}

/**
 * The label string `wallpaper_<name>`, looked up by name so a new image needs only that string (D-038);
 * falls back to the file name. The strings are kept in release builds by res/raw/zeroos_keep.xml.
 */
@SuppressLint("DiscouragedApi")
@Composable
internal fun wallpaperLabel(name: String): String {
    val resources = LocalResources.current
    val packageName = LocalContext.current.packageName
    val id = remember(name, resources) { resources.getIdentifier("$LABEL_PREFIX$name", "string", packageName) }
    return if (id != 0) stringResource(id) else name.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private const val LABEL_PREFIX = "wallpaper_"
private const val GALLERY_COLUMNS = 3

/** Thumbnails have the phone's shape (9:20). */
private const val THUMB_WIDTH = 9
private const val THUMB_HEIGHT = 20
