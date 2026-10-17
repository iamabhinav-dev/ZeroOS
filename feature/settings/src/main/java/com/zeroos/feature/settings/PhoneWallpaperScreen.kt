package com.zeroos.feature.settings

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.zeroos.core.ui.DotIcons
import com.zeroos.core.ui.DotMatrixIcon
import com.zeroos.core.ui.MinimalListItem
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
            onOpen = viewModel::open,
            onClosePreview = viewModel::closePreview,
            onShowApply = viewModel::showApply,
            onDismissApply = viewModel::dismissApply,
            onApply = viewModel::apply,
            onRestore = viewModel::restorePrevious
        ),
        image = { item, width, height -> viewModel.thumbnail(item, width, height).asImageBitmap() },
        onBack = onBack
    )
}

data class PhoneWallpaperActions(
    val onOpen: (WallpaperItem) -> Unit,
    val onClosePreview: () -> Unit,
    val onShowApply: () -> Unit,
    val onDismissApply: () -> Unit,
    val onApply: (Set<WallpaperScreen>) -> Unit,
    val onRestore: (WallpaperScreen) -> Unit
)

/**
 * Wireframes F5b (gallery) and F5c (full-screen preview with Apply). [image] draws a wallpaper at the given
 * pixel size (off the main thread in the app).
 */
@Composable
fun PhoneWallpaperScreen(
    state: PhoneWallpaperState,
    actions: PhoneWallpaperActions,
    image: suspend (WallpaperItem, Int, Int) -> ImageBitmap?,
    onBack: () -> Unit
) {
    val previewing = state.previewing
    if (previewing != null) {
        WallpaperPreview(previewing, state.choosing, actions, image)
        return
    }
    SettingsPageScaffold(stringResource(R.string.phone_wallpaper_title), SettingsTestTags.PHONE_WALLPAPER, onBack) {
        state.items.chunked(GALLERY_COLUMNS).forEach { row ->
            item(key = row.first().id) { GalleryRow(row, state.currentHomeId, image, actions.onOpen) }
        }
        item { BodyText(status(state), muted = true, small = true) }
        state.result?.let { outcome -> outcomeLines(outcome).forEach { line -> item { BodyText(stringResource(line)) } } }
        if (state.history.previous(WallpaperScreen.HOME) != null) {
            item { NavRow(R.string.phone_wallpaper_restore_home) { actions.onRestore(WallpaperScreen.HOME) } }
        }
        if (state.history.previous(WallpaperScreen.LOCK) != null) {
            item { NavRow(R.string.phone_wallpaper_restore_lock) { actions.onRestore(WallpaperScreen.LOCK) } }
        }
        item { BodyText(stringResource(R.string.phone_wallpaper_note), muted = true, small = true) }
    }
}

/** Wireframe F5c: the wallpaper full screen, cropped as it will be, with a back arrow and Apply. */
@Composable
private fun WallpaperPreview(
    item: WallpaperItem,
    choosing: Boolean,
    actions: PhoneWallpaperActions,
    image: suspend (WallpaperItem, Int, Int) -> ImageBitmap?
) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = actions.onClosePreview)
    val size = LocalWindowInfo.current.containerSize
    val bitmap by produceState<ImageBitmap?>(null, item.id, size) {
        if (size.width > 0 && size.height > 0) value = image(item, size.width, size.height)
    }
    Box(Modifier.fillMaxSize().background(tokens.colors.background).testTag(SettingsTestTags.WALLPAPER_PREVIEW)) {
        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(
                Modifier
                    .size(tokens.spacing.minTouchTarget)
                    .clickable(role = Role.Button, onClick = actions.onClosePreview),
                contentAlignment = Alignment.Center
            ) {
                DotMatrixIcon(
                    DotIcons.Back,
                    height = tokens.spacing.minTouchTarget / 2,
                    contentDescription = stringResource(R.string.common_back)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                wallpaperLabel(item.name),
                color = tokens.colors.text,
                fontSize = tokens.textSizes.body,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            TextAction(
                stringResource(R.string.phone_wallpaper_apply),
                onClick = actions.onShowApply,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
    if (choosing) ApplyDialog(actions)
}

/** Apply → Home screen, Lock screen or Both; a screen not chosen is left unchanged (F6.8). */
@Composable
private fun ApplyDialog(actions: PhoneWallpaperActions) {
    AlertDialog(
        onDismissRequest = actions.onDismissApply,
        containerColor = ZeroTheme.tokens.colors.background,
        title = { Text(stringResource(R.string.phone_wallpaper_apply_title)) },
        text = {
            Column {
                MinimalListItem(stringResource(R.string.phone_wallpaper_home_screen), { actions.onApply(setOf(WallpaperScreen.HOME)) })
                MinimalListItem(stringResource(R.string.phone_wallpaper_lock_screen), { actions.onApply(setOf(WallpaperScreen.LOCK)) })
                MinimalListItem(stringResource(R.string.phone_wallpaper_both), { actions.onApply(WallpaperScreen.entries.toSet()) })
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun GalleryRow(
    row: List<WallpaperItem>,
    selectedId: String?,
    thumbnail: suspend (WallpaperItem, Int, Int) -> ImageBitmap?,
    onOpen: (WallpaperItem) -> Unit
) {
    val tokens = ZeroTheme.tokens
    Row(
        Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.sectionGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.screenGutter)
    ) {
        row.forEach { GalleryItem(it, it.id == selectedId, thumbnail, onOpen, Modifier.weight(1f)) }
        repeat(GALLERY_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun GalleryItem(
    item: WallpaperItem,
    selected: Boolean,
    thumbnail: suspend (WallpaperItem, Int, Int) -> ImageBitmap?,
    onOpen: (WallpaperItem) -> Unit,
    modifier: Modifier
) {
    val tokens = ZeroTheme.tokens
    val label = wallpaperLabel(item.name)
    val density = LocalDensity.current
    val width = with(density) { tokens.spacing.thumbnailWidth.roundToPx() }
    val image by produceState<ImageBitmap?>(null, item.id) { value = thumbnail(item, width, width * THUMB_HEIGHT / THUMB_WIDTH) }
    Column(
        modifier
            .clickable(onClickLabel = label, role = Role.Button) { onOpen(item) }
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
