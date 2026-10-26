package com.zeroos.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.AppRule
import com.zeroos.core.model.settings.DisableFriction
import com.zeroos.core.model.settings.WellbeingSettings
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BackTitle
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.HoldToConfirm
import com.zeroos.core.ui.MinimalBottomSheet
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.PermissionRequest
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.SearchField
import com.zeroos.core.ui.SettingRow
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ToggleRow
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.displayName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * Settings → Wellbeing and its Apps page (F8.8–F8.12, wireframes H3, H5). Switching Wellbeing or daily limits on
 * asks for Usage access first (F8.11); switching off while a limit is active asks for the chosen friction (F8.12).
 */
@Composable
fun WellbeingRoute(
    showApps: Boolean,
    onOpenApps: () -> Unit,
    onOpenFocus: () -> Unit,
    onBack: () -> Unit,
    onOpenShortForm: () -> Unit = {},
    viewModel: WellbeingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var askingFor by rememberSaveable { mutableStateOf<Pair<WellbeingToggle, SpecialPermission>?>(null) }
    var confirmingOff by rememberSaveable { mutableStateOf<WellbeingToggle?>(null) }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose {}
    }
    val permissions = viewModel.permissions
    fun needed(toggle: WellbeingToggle) = neededPermission(
        toggle,
        state.settings,
        permissions.isGranted(SpecialPermission.USAGE_ACCESS),
        permissions.isGranted(SpecialPermission.ACCESSIBILITY)
    )
    askingFor?.let { (toggle, permission) ->
        PermissionRequest(
            permission = permission,
            isGranted = { permissions.isGranted(permission) },
            openSettings = { permissions.openSettings(permission) },
            openAppInfo = permissions::openAppInfo,
            onResult = { granted ->
                viewModel.onPermissionResult(permission, granted, toggle)
                // Still missing one (take a break needs both): ask for it next.
                askingFor = if (granted) needed(toggle)?.let { toggle to it } else null
            }
        )
        return
    }
    if (showApps) {
        WellbeingAppsScreen(state, viewModel::onQueryChange, viewModel::setAppFlag, viewModel::cycleAppLimit, onBack)
        return
    }
    WellbeingScreen(
        settings = state.settings,
        actions = WellbeingActions(
            onToggle = { toggle, on ->
                when (toggleOutcome(state.settings, needed(toggle), toggle, on)) {
                    ToggleOutcome.CONFIRM_OFF -> confirmingOff = toggle
                    ToggleOutcome.ASK -> askingFor = needed(toggle)?.let { toggle to it }
                    ToggleOutcome.APPLY -> viewModel.setToggle(toggle, on)
                }
            },
            onCycle = viewModel::cycle,
            onOpenApps = onOpenApps,
            onOpenFocus = onOpenFocus,
            onOpenShortForm = onOpenShortForm
        ),
        onBack = onBack
    )
    confirmingOff?.let { toggle ->
        TurnOffDialog(
            friction = state.settings.disableFriction,
            pauseSeconds = state.settings.pauseSeconds,
            onConfirm = {
                confirmingOff = null
                viewModel.setToggle(toggle, false)
            },
            onDismiss = { confirmingOff = null }
        )
    }
}

/** What switching a Wellbeing row does (F8.11, F8.12). */
enum class ToggleOutcome { APPLY, ASK, CONFIRM_OFF }

/**
 * Off while a daily limit is active (Wellbeing or limits) needs the friction; on with a [missing] permission asks
 * for it first (see [neededPermission]); everything else applies at once.
 */
internal fun toggleOutcome(settings: WellbeingSettings, missing: SpecialPermission?, toggle: WellbeingToggle, on: Boolean): ToggleOutcome {
    val guarded = toggle == WellbeingToggle.ENABLED || toggle == WellbeingToggle.LIMITS
    return when {
        guarded && !on && settings.hasActiveLimit && settings.disableFriction != DisableFriction.OFF -> ToggleOutcome.CONFIRM_OFF
        on && missing != null -> ToggleOutcome.ASK
        else -> ToggleOutcome.APPLY
    }
}

data class WellbeingActions(
    val onToggle: (WellbeingToggle, Boolean) -> Unit,
    val onCycle: (WellbeingCycle) -> Unit,
    val onOpenApps: () -> Unit,
    val onOpenFocus: () -> Unit = {},
    val onOpenShortForm: () -> Unit = {}
)

/** Wireframe H3. With Wellbeing off, only the master switch and a note are shown (F8.8, F8.10). */
@Composable
fun WellbeingScreen(settings: WellbeingSettings, actions: WellbeingActions, onBack: () -> Unit) {
    SettingsPageScaffold(stringResource(R.string.settings_wellbeing), SettingsTestTags.WELLBEING, onBack) {
        item { ToggleRow(stringResource(R.string.settings_wellbeing), settings.enabled, { actions.onToggle(WellbeingToggle.ENABLED, it) }) }
        if (!settings.enabled) {
            item { BodyText(stringResource(R.string.wellbeing_off_note), muted = true, small = true) }
            // Focus and the short-video blocker work without Wellbeing (F9.10, F16.11).
            item { NavRow(R.string.focus_title, actions.onOpenFocus) }
            item { NavRow(R.string.shorts_title, actions.onOpenShortForm) }
            return@SettingsPageScaffold
        }
        item { ToggleRow(stringResource(R.string.wellbeing_pause), settings.pauseEnabled, { actions.onToggle(WellbeingToggle.PAUSE, it) }) }
        item {
            SettingRow(
                stringResource(R.string.wellbeing_pause_length),
                stringResource(R.string.wellbeing_seconds, settings.pauseSeconds),
                { actions.onCycle(WellbeingCycle.PAUSE_SECONDS) }
            )
        }
        item {
            ToggleRow(stringResource(R.string.wellbeing_intention), settings.intentionPrompt, {
                actions.onToggle(WellbeingToggle.INTENTION, it)
            })
        }
        item {
            ToggleRow(stringResource(R.string.wellbeing_limits), settings.limitsEnabled, { actions.onToggle(WellbeingToggle.LIMITS, it) })
        }
        item { NavRow(R.string.wellbeing_apps, actions.onOpenApps) }
        item {
            SettingRow(
                stringResource(R.string.wellbeing_extra),
                stringResource(R.string.wellbeing_extra_value, settings.extraMinutesPerDay),
                { actions.onCycle(WellbeingCycle.EXTRA_MINUTES) }
            )
        }
        item {
            ToggleRow(stringResource(R.string.wellbeing_outside), settings.outsideEnforcement, {
                actions.onToggle(WellbeingToggle.OUTSIDE, it)
            })
        }
        item {
            ToggleRow(stringResource(R.string.wellbeing_breaks), settings.breaksEnabled, { actions.onToggle(WellbeingToggle.BREAKS, it) })
        }
        item {
            SettingRow(
                stringResource(R.string.wellbeing_break_after),
                stringResource(R.string.wellbeing_minutes, settings.breakAfterMinutes),
                { actions.onCycle(WellbeingCycle.BREAK_AFTER) }
            )
        }
        item {
            SettingRow(
                stringResource(R.string.wellbeing_break_wait),
                stringResource(R.string.wellbeing_seconds, settings.breakWaitSeconds),
                { actions.onCycle(WellbeingCycle.BREAK_WAIT) }
            )
        }
        item {
            SettingRow(stringResource(R.string.wellbeing_friction), frictionLabel(settings.disableFriction), {
                actions.onCycle(WellbeingCycle.FRICTION)
            })
        }
        item { NavRow(R.string.focus_title, actions.onOpenFocus) }
        item { NavRow(R.string.shorts_title, actions.onOpenShortForm) }
    }
}

/** Wireframes H5, H5a and H14: apps with a rule first; tap one for its pause, daily limit and take a break. */
@Composable
fun WellbeingAppsScreen(
    state: WellbeingUiState,
    onQueryChange: (String) -> Unit,
    onFlag: (String, AppFlag, Boolean) -> Unit,
    onCycleLimit: (String) -> Unit,
    onBack: () -> Unit
) {
    val tokens = ZeroTheme.tokens
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(SettingsTestTags.WELLBEING_APPS)) {
        BackTitle(stringResource(R.string.wellbeing_apps), onBack)
        SearchField(state.query, autoOpenKeyboard = false, onQueryChange = onQueryChange, onGo = {})
        HorizontalDivider(color = tokens.colors.divider)
        LazyColumn {
            items(state.apps, key = { it.app.key.packageName }) { row ->
                MinimalListItem(
                    text = row.app.displayName(),
                    onClick = { open = row.app.key.packageName },
                    trailing = ruleSummary(row.rule),
                    large = true
                )
            }
        }
    }
    val sheet = state.apps.firstOrNull { it.app.key.packageName == open }
    if (sheet != null) {
        MinimalBottomSheet(onDismiss = { open = null }) {
            ScreenTitle(sheet.app.displayName())
            ToggleRow(stringResource(R.string.wellbeing_pause), sheet.rule.pause, { onFlag(sheet.app.key.packageName, AppFlag.PAUSE, it) })
            SettingRow(
                stringResource(R.string.wellbeing_daily_limit),
                sheet.rule.dailyLimitMinutes?.let { stringResource(R.string.wellbeing_minutes, it) } ?: stringResource(R.string.common_off),
                { onCycleLimit(sheet.app.key.packageName) }
            )
            ToggleRow(stringResource(R.string.wellbeing_breaks), sheet.rule.takeBreak, {
                onFlag(sheet.app.key.packageName, AppFlag.BREAK, it)
            })
        }
    }
}

/** F8.12: hold for 3 s, or wait out a pause, before Wellbeing or limits turn off. */
@Composable
private fun TurnOffDialog(friction: DisableFriction, pauseSeconds: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var left by rememberSaveable { mutableIntStateOf(pauseSeconds) }
    if (friction == DisableFriction.PAUSE) {
        LaunchedEffect(Unit) {
            while (left > 0) {
                delay(1.seconds)
                left--
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZeroTheme.tokens.colors.background,
        title = { Text(stringResource(R.string.wellbeing_turn_off_title)) },
        text = {
            when {
                friction == DisableFriction.HOLD -> HoldToConfirm(
                    stringResource(R.string.wellbeing_hold_to_turn_off),
                    HOLD_TO_TURN_OFF,
                    onConfirm
                )

                left > 0 -> BodyText(stringResource(R.string.wellbeing_turn_off_in, left))

                else -> TextAction(stringResource(R.string.wellbeing_turn_off), onClick = onConfirm)
            }
        },
        confirmButton = {},
        dismissButton = { TextAction(stringResource(R.string.common_cancel), onClick = onDismiss, compact = true) }
    )
}

@Composable
private fun frictionLabel(friction: DisableFriction): String = when (friction) {
    DisableFriction.OFF -> stringResource(R.string.common_off)
    DisableFriction.HOLD -> stringResource(R.string.wellbeing_friction_hold, HOLD_TO_TURN_OFF.inWholeSeconds.toInt())
    DisableFriction.PAUSE -> stringResource(R.string.wellbeing_friction_pause)
}

/** "pause · 30 min · break", "30 min", "pause", or nothing (H5). */
@Composable
private fun ruleSummary(rule: AppRule): String? {
    val parts = listOfNotNull(
        if (rule.pause) stringResource(R.string.wellbeing_rule_pause) else null,
        rule.dailyLimitMinutes?.let { stringResource(R.string.wellbeing_minutes, it) },
        if (rule.takeBreak) stringResource(R.string.wellbeing_rule_break) else null
    )
    return parts.reduceOrNull { a, b -> stringResource(R.string.wellbeing_rule_join, a, b) }
}

/** F8.12, Appendix A: hold 3 s. */
private val HOLD_TO_TURN_OFF = 3.seconds
