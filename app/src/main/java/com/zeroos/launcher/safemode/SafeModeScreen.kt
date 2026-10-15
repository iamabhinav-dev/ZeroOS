package com.zeroos.launcher.safemode

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.zeroos.core.resources.R
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ZeroTheme

/** Safe mode (F18.3, wireframe D): banner, three actions, plain alphabetical app list. */
@Composable
fun SafeModeScreen(
    apps: List<SafeModeApp>,
    onAppClick: (SafeModeApp) -> Unit,
    onViewReport: () -> Unit,
    onHomeSettings: () -> Unit,
    onExit: () -> Unit
) {
    val tokens = ZeroTheme.tokens
    BackHandler {}
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .safeDrawingPadding()
            .testTag(SafeModeTestTags.ROOT)
    ) {
        item {
            Text(
                text = stringResource(R.string.safe_mode_title),
                color = tokens.colors.text,
                fontSize = tokens.textSizes.title,
                modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.sectionGap)
            )
            Text(
                text = stringResource(R.string.safe_mode_message),
                color = tokens.colors.textMuted,
                fontSize = tokens.textSizes.body,
                modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter)
            )
        }
        item { MinimalListItem(text = stringResource(R.string.safe_mode_view_report), onClick = onViewReport, large = true) }
        item { MinimalListItem(text = stringResource(R.string.safe_mode_home_settings), onClick = onHomeSettings, large = true) }
        item { MinimalListItem(text = stringResource(R.string.safe_mode_exit), onClick = onExit, large = true) }
        item { HorizontalDivider(color = tokens.colors.divider) }
        items(apps, key = { "${it.component.flattenToString()}#${it.user.hashCode()}" }) { app ->
            MinimalListItem(text = app.label, onClick = { onAppClick(app) }, large = true)
        }
    }
}

object SafeModeTestTags {
    const val ROOT = "safe_mode"
}
