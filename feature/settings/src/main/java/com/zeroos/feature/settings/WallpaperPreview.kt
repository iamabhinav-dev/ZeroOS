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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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

/** Wireframe F5c: the wallpaper full screen, cropped as it will be, with a back arrow and Apply. */
@Composable
internal fun WallpaperPreview(
    items: List<WallpaperItem>,
    item: WallpaperItem,
    choosing: Boolean,
    actions: PhoneWallpaperActions,
    image: suspend (WallpaperItem, Int, Int) -> ImageBitmap?
) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = actions.onClosePreview)
    // Swipe left/right to the neighbouring wallpapers; stops at the first and last (no wrapping around).
    val pager = rememberPagerState(initialPage = items.indexOf(item).coerceAtLeast(0)) { items.size }
    val onOpen by rememberUpdatedState(actions.onOpen)
    val showing by rememberUpdatedState(item)
    LaunchedEffect(pager, items) {
        snapshotFlow { pager.currentPage }.collect { page -> items.getOrNull(page)?.let { if (it.id != showing.id) onOpen(it) } }
    }
    Box(Modifier.fillMaxSize().background(tokens.colors.background).testTag(SettingsTestTags.WALLPAPER_PREVIEW)) {
        HorizontalPager(state = pager, key = { items[it].id }, modifier = Modifier.fillMaxSize()) { page ->
            PreviewImage(items[page], image)
        }
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

/** One wallpaper drawn at the window's size, cropped as it will be on the phone. */
@Composable
private fun PreviewImage(item: WallpaperItem, image: suspend (WallpaperItem, Int, Int) -> ImageBitmap?) {
    val size = LocalWindowInfo.current.containerSize
    val bitmap by produceState<ImageBitmap?>(null, item.id, size) {
        if (size.width > 0 && size.height > 0) value = image(item, size.width, size.height)
    }
    Box(Modifier.fillMaxSize().testTag(SettingsTestTags.wallpaperPage(item.id))) {
        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
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
