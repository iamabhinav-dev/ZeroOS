package com.zeroos.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.zeroos.core.resources.R

/** What the app menu needs to know about the app (wireframe E1). */
data class AppMenuTarget(
    val label: String,
    val isFavorite: Boolean,
    val isRenamed: Boolean,
    /** System apps can't be uninstalled, so "Uninstall" is left out (F2.8). */
    val canUninstall: Boolean = true
)

/** Callbacks of the app menu. */
data class AppMenuActions(
    val onToggleFavorite: () -> Unit,
    val onRename: (String?) -> Unit,
    val onHide: () -> Unit,
    val onAppInfo: () -> Unit,
    val onUninstall: () -> Unit
)

/**
 * Long-press menu for an app (F2.8, wireframe E1) and its rename dialog (F5.4, F5.5, wireframe E2).
 * "Set limit" joins in Phase 4.
 */
@Composable
fun AppMenu(target: AppMenuTarget, actions: AppMenuActions, onDismiss: () -> Unit) {
    var renaming by rememberSaveable { mutableStateOf(false) }
    if (renaming) {
        RenameDialog(
            target = target,
            onSave = {
                actions.onRename(it)
                onDismiss()
            },
            onDismiss = onDismiss
        )
        return
    }
    val tokens = ZeroTheme.tokens
    MinimalBottomSheet(onDismiss = onDismiss) {
        Column(Modifier.testTag(AppMenuTestTags.SHEET)) {
            ScreenTitle(target.label)
            HorizontalDivider(color = tokens.colors.divider, modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter))
            val favorite = if (target.isFavorite) R.string.app_menu_remove_favorite else R.string.app_menu_add_favorite
            MinimalListItem(stringResource(favorite), onClick = { actions.onToggleFavorite().also { onDismiss() } })
            MinimalListItem(stringResource(R.string.app_menu_rename), onClick = { renaming = true })
            MinimalListItem(stringResource(R.string.app_menu_hide), onClick = { actions.onHide().also { onDismiss() } })
            MinimalListItem(stringResource(R.string.app_menu_app_info), onClick = { actions.onAppInfo().also { onDismiss() } })
            if (target.canUninstall) {
                MinimalListItem(stringResource(R.string.app_menu_uninstall), onClick = { actions.onUninstall().also { onDismiss() } })
            }
        }
    }
}

/** Wireframe E2. Save is disabled for an empty name; "Reset to original" shows only for renamed apps. */
@Composable
fun RenameDialog(target: AppMenuTarget, onSave: (String?) -> Unit, onDismiss: () -> Unit) {
    val tokens = ZeroTheme.tokens
    var name by rememberSaveable { mutableStateOf(target.label) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.colors.background,
        title = { androidx.compose.material3.Text(stringResource(R.string.rename_title, target.label)) },
        text = {
            Column {
                val style = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.text, fontSize = tokens.textSizes.appLabel)
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(tokens.colors.text),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onSave(name) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag(AppMenuTestTags.RENAME_FIELD)
                )
                HorizontalDivider(color = tokens.colors.textMuted)
                if (target.isRenamed) {
                    Box(Modifier.padding(top = tokens.spacing.sectionGap)) {
                        TextAction(stringResource(R.string.rename_reset), onClick = { onSave(null) }, compact = true)
                    }
                }
            }
        },
        confirmButton = {
            TextAction(stringResource(R.string.common_save), onClick = {
                if (name.isNotBlank()) onSave(name)
            }, muted = name.isBlank(), compact = true)
        },
        dismissButton = { TextAction(stringResource(R.string.common_cancel), onClick = onDismiss, compact = true) }
    )
}

/** Same as the data-layer limit (wireframe E2). */
private const val MAX_NAME_LENGTH = 40

object AppMenuTestTags {
    const val SHEET = "app_menu"
    const val RENAME_FIELD = "rename_field"
}
