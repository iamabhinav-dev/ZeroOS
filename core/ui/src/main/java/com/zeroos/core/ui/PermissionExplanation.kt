package com.zeroos.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.resources.R

/** Callbacks of the permission explanation screen. */
data class PermissionActions(val onOpenSettings: () -> Unit, val onOpenAppInfo: () -> Unit, val onNotNow: () -> Unit)

/**
 * Explains a special permission before opening Android's screen for it (wireframe H2, F14.4, NFR-S4). Accessibility
 * also shows the "Allow restricted settings" steps for sideloaded apps, what it reads, and the UPI-app note.
 * [notYet] adds "Not allowed yet" after a return without the permission.
 */
@Composable
fun PermissionExplanation(permission: SpecialPermission, notYet: Boolean, actions: PermissionActions) {
    BackHandler(onBack = actions.onNotNow)
    LazyColumn(
        Modifier.fillMaxSize().background(ZeroTheme.tokens.colors.background).safeDrawingPadding().testTag(PermissionTestTags.SCREEN)
    ) {
        item { ScreenTitle(stringResource(permission.title())) }
        if (notYet) item { BodyText(stringResource(R.string.permission_not_yet)) }
        item { BodyText(stringResource(permission.why())) }
        item { BodyText(stringResource(R.string.permission_private), muted = true) }
        if (permission == SpecialPermission.ACCESSIBILITY) {
            item { BodyText(stringResource(R.string.permission_accessibility_reads)) }
            item { BodyText(stringResource(R.string.permission_accessibility_upi), muted = true) }
            item { BodyText(stringResource(R.string.permission_restricted_intro)) }
            RESTRICTED_STEPS.forEachIndexed { index, step ->
                item {
                    val text = stringResource(R.string.permission_step, index + 1, stringResource(step))
                    if (index == 0) MinimalListItem(text, onClick = actions.onOpenAppInfo) else BodyText(text)
                }
            }
        }
        item { MinimalListItem(stringResource(R.string.permission_open_settings), onClick = actions.onOpenSettings, trailing = CHEVRON) }
        item { MinimalListItem(stringResource(R.string.permission_not_now), onClick = actions.onNotNow, trailing = CHEVRON) }
    }
}

/**
 * The whole request (Step 4.1): explanation → Android's screen → check again when ZeroOS is back in front.
 * [onResult] gets true once [isGranted] says so, or false on "Not now".
 */
@Composable
fun PermissionRequest(
    permission: SpecialPermission,
    isGranted: () -> Boolean,
    openSettings: () -> Unit,
    openAppInfo: () -> Unit,
    onResult: (Boolean) -> Unit
) {
    var waiting by rememberSaveable { mutableStateOf(false) }
    var notYet by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(waiting) {
        if (waiting) {
            if (isGranted()) onResult(true) else notYet = true
            waiting = false
        }
        onPauseOrDispose {}
    }
    PermissionExplanation(
        permission,
        notYet,
        PermissionActions(
            onOpenSettings = {
                waiting = true
                openSettings()
            },
            onOpenAppInfo = {
                waiting = true
                openAppInfo()
            },
            onNotNow = { onResult(false) }
        )
    )
}

private fun SpecialPermission.title() = when (this) {
    SpecialPermission.USAGE_ACCESS -> R.string.permission_usage_title
    SpecialPermission.ACCESSIBILITY -> R.string.permission_accessibility_title
    SpecialPermission.DO_NOT_DISTURB -> R.string.permission_dnd_title
}

private fun SpecialPermission.why() = when (this) {
    SpecialPermission.USAGE_ACCESS -> R.string.permission_usage_why
    SpecialPermission.ACCESSIBILITY -> R.string.permission_accessibility_why
    SpecialPermission.DO_NOT_DISTURB -> R.string.permission_dnd_why
}

private val RESTRICTED_STEPS = listOf(
    R.string.permission_restricted_open_app_info,
    R.string.permission_restricted_menu,
    R.string.permission_restricted_allow,
    R.string.permission_restricted_retry
)

/** Doto has "›". */
private const val CHEVRON = "›"

object PermissionTestTags {
    const val SCREEN = "permission_explanation"
}
