package com.zeroos.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.zeroos.core.resources.R

// Shared building blocks (Step 2.2): plain text, monochrome, at least 48 dp tall when tappable (NFR-A2).

/** Large screen title, e.g. "Settings". */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    Text(
        text = text,
        color = tokens.colors.text,
        fontSize = tokens.textSizes.title,
        modifier = modifier.padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.sectionGap)
    )
}

/** Paragraph or caption text. */
@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, muted: Boolean = false, small: Boolean = false) {
    val tokens = ZeroTheme.tokens
    Text(
        text = text,
        color = if (muted) tokens.colors.textMuted else tokens.colors.text,
        fontSize = if (small) tokens.textSizes.small else tokens.textSizes.body,
        modifier = modifier.padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
    )
}

/** An app name as shown in lists and on home (F1.4, F2.2). */
@Composable
fun AppLabel(text: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    val tokens = ZeroTheme.tokens
    Text(
        text = text,
        color = if (muted) tokens.colors.textMuted else tokens.colors.text,
        fontSize = tokens.textSizes.appLabel,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** A full-width tappable row of text, optionally long-pressable, with optional trailing text (e.g. "›"). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalListItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    muted: Boolean = false,
    trailing: String? = null,
    large: Boolean = false
) {
    val tokens = ZeroTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.minTouchTarget)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (muted) tokens.colors.textMuted else tokens.colors.text,
            fontSize = if (large) tokens.textSizes.appLabel else tokens.textSizes.body,
            modifier = Modifier.weight(1f)
        )
        trailing?.let { Text(text = it, color = tokens.colors.textMuted, fontSize = tokens.textSizes.body) }
    }
}

/** A setting with its current value on the right; tap to change (wireframe F). */
@Composable
fun SettingRow(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    MinimalListItem(text = label, onClick = onClick, modifier = modifier, trailing = value)

/** An on/off setting. Announced to screen readers as a switch with its state (NFR-A1). */
@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val state = stringResource(if (checked) R.string.common_on else R.string.common_off)
    MinimalListItem(
        text = label,
        onClick = { onCheckedChange(!checked) },
        trailing = state,
        modifier = modifier.semantics {
            role = Role.Switch
            stateDescription = state
        }
    )
}

/** A short text button, e.g. "Export logs". [compact] uses less side padding for rows of several buttons. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, muted: Boolean = false, compact: Boolean = false) {
    val tokens = ZeroTheme.tokens
    Box(
        modifier = modifier
            .heightIn(min = tokens.spacing.minTouchTarget)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = if (compact) tokens.spacing.sectionGap else tokens.spacing.screenGutter),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, color = if (muted) tokens.colors.textMuted else tokens.colors.text, fontSize = tokens.textSizes.body)
    }
}

/** Bottom sheet in the ZeroOS style, e.g. the app long-press menu (wireframe E1). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val tokens = ZeroTheme.tokens
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.colors.background,
        contentColor = tokens.colors.text
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = tokens.spacing.screenGutter), content = content)
    }
}
