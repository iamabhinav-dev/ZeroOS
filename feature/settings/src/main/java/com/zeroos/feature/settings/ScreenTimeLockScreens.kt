package com.zeroos.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BackTitle
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.DotLine
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.PermissionRequest
import com.zeroos.core.ui.SearchField
import com.zeroos.core.ui.SettingRow
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.displayName
import com.zeroos.core.ui.durationText
import kotlinx.coroutines.launch

/** Settings → Wellbeing → Screen-time lock and its essential-apps picker (F19.1, F19.6, F19.7, wireframes L1, L2). */
@Composable
fun ScreenTimeLockRoute(
    showApps: Boolean,
    onOpenApps: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScreenTimeLockSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val permissions = viewModel.permissions
    var asking by rememberSaveable { mutableStateOf<SpecialPermission?>(null) }
    var pending by remember { mutableStateOf<LockChange?>(null) }
    var wrong by remember { mutableStateOf(false) }
    var editingPhrase by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    fun request(change: LockChange) {
        if (viewModel.needsPhrase()) pending = change else scope.launch { viewModel.confirm(change, null) }
    }
    asking?.let { permission ->
        PermissionRequest(
            permission = permission,
            isGranted = { permissions.isGranted(permission) },
            openSettings = { permissions.openSettings(permission) },
            openAppInfo = permissions::openAppInfo,
            onResult = { granted ->
                permissions.record(permission, granted)
                asking = nextTurnOnStep(permission, granted, permissions.isGranted(SpecialPermission.ACCESSIBILITY))
                // Usage access is needed to count screen time; Accessibility is optional (F19.7).
                if (asking == null && (granted || permission == SpecialPermission.ACCESSIBILITY)) viewModel.turnOn()
            }
        )
        return
    }
    if (showApps) {
        EssentialAppsScreen(state, viewModel::onQueryChange, onToggle = { key, on ->
            if (on) request(LockChange.AddApp(key)) else viewModel.removeApp(key)
        }, onBack = onBack)
    } else {
        ScreenTimeLockScreen(
            state,
            LockSettingsActions(
                onTurnOn = {
                    asking =
                        firstMissing(
                            permissions.isGranted(SpecialPermission.USAGE_ACCESS),
                            permissions.isGranted(SpecialPermission.ACCESSIBILITY)
                        )
                    if (asking == null) viewModel.turnOn()
                },
                onTurnOff = { request(LockChange.TurnOff) },
                onLowerCap = viewModel::lowerCap,
                onRaiseCap = { request(LockChange.RaiseCap) },
                onOpenApps = onOpenApps,
                onPhrase = { editingPhrase = true }
            ),
            onBack
        )
    }
    if (editingPhrase) {
        NewPhraseDialog(state.settings.phrase, onSave = {
            editingPhrase = false
            request(LockChange.NewPhrase(it))
        }, onDismiss = { editingPhrase = false })
    }
    pending?.let { change ->
        PhraseDialog(state.settings.phrase, wrong, onConfirm = { typed ->
            scope.launch {
                wrong = !viewModel.confirm(change, typed)
                if (!wrong) pending = null
            }
        }, onDismiss = {
            pending = null
            wrong = false
        })
    }
}

/** What turning the lock on asks for first: Usage access, then Accessibility (F19.7). */
internal fun firstMissing(usageAccess: Boolean, accessibility: Boolean): SpecialPermission? = when {
    !usageAccess -> SpecialPermission.USAGE_ACCESS
    !accessibility -> SpecialPermission.ACCESSIBILITY
    else -> null
}

/** After a permission answer while turning the lock on: Accessibility comes next if Usage access was given. */
internal fun nextTurnOnStep(answered: SpecialPermission, granted: Boolean, accessibilityOn: Boolean): SpecialPermission? =
    SpecialPermission.ACCESSIBILITY.takeIf { answered == SpecialPermission.USAGE_ACCESS && granted && !accessibilityOn }

data class LockSettingsActions(
    val onTurnOn: () -> Unit,
    val onTurnOff: () -> Unit,
    val onLowerCap: () -> Unit,
    val onRaiseCap: () -> Unit,
    val onOpenApps: () -> Unit,
    val onPhrase: () -> Unit
)

/** Wireframe L1. */
@Composable
fun ScreenTimeLockScreen(state: LockSettingsState, actions: LockSettingsActions, onBack: () -> Unit) {
    val lock = state.settings
    SettingsPageScaffold(stringResource(R.string.lock_settings_title), SettingsTestTags.SCREEN_TIME_LOCK, onBack) {
        if (!lock.enabled) item { BodyText(stringResource(R.string.lock_settings_off_note), muted = true, small = true) }
        item { CapRow(lock.capMinutes, actions.onLowerCap, actions.onRaiseCap) }
        item {
            SettingRow(
                stringResource(R.string.lock_settings_essential),
                pluralStringResource(R.plurals.lock_essential_count, lock.essentialApps.size, lock.essentialApps.size),
                actions.onOpenApps
            )
        }
        item { SettingRow(stringResource(R.string.lock_settings_phrase), lock.phrase, actions.onPhrase) }
        if (lock.enabled) {
            item { TodayLine(state.usedMillis, lock.capMinutes) }
            if (!state.accessibilityOn) {
                item {
                    BodyText(stringResource(R.string.lock_settings_no_accessibility), muted = true, small = true)
                }
            }
            item { NavRow(R.string.lock_settings_turn_off, actions.onTurnOff) }
        } else {
            item { NavRow(R.string.lock_settings_turn_on, actions.onTurnOn) }
        }
        item { BodyText(stringResource(R.string.lock_settings_note), muted = true, small = true) }
    }
}

/** "Lock after ‹ 4h ›": ‹ makes it stricter at once; › asks for the phrase while the lock is on (F19.6). */
@Composable
private fun CapRow(minutes: Int, onLower: () -> Unit, onRaise: () -> Unit) {
    val tokens = ZeroTheme.tokens
    val shorter = stringResource(R.string.focus_shorter)
    val longer = stringResource(R.string.focus_longer)
    Row(
        Modifier.fillMaxWidth().heightIn(min = tokens.spacing.minTouchTarget).padding(start = tokens.spacing.screenGutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.lock_settings_cap),
            color = tokens.colors.text,
            fontSize = tokens.textSizes.body,
            modifier = Modifier.weight(1f)
        )
        TextAction(
            STEP_DOWN,
            onLower,
            modifier = Modifier.semantics { contentDescription = shorter },
            compact = true,
            enabled = minutes > ScreenTimeLockSettingsViewModel.CAP_MIN
        )
        Text(durationText(minutes * MILLIS_PER_MINUTE), color = tokens.colors.text, fontSize = tokens.textSizes.body)
        TextAction(
            STEP_UP,
            onRaise,
            modifier = Modifier.semantics { contentDescription = longer }.testTag(SettingsTestTags.LOCK_RAISE),
            compact = true,
            enabled = minutes < ScreenTimeLockSettingsViewModel.CAP_MAX
        )
    }
}

/** "Today: 1h 52m of 4h" with the dotted line filling up, as on home (F10.5). */
@Composable
private fun TodayLine(usedMillis: Long, capMinutes: Int) {
    val cap = capMinutes * MILLIS_PER_MINUTE
    Column(Modifier.padding(horizontal = ZeroTheme.tokens.spacing.screenGutter, vertical = ZeroTheme.tokens.spacing.sectionGap)) {
        Text(
            stringResource(R.string.lock_settings_today, durationText(usedMillis), durationText(cap)),
            color = ZeroTheme.tokens.colors.text,
            fontSize = ZeroTheme.tokens.textSizes.body
        )
        DotLine((usedMillis.toFloat() / cap).coerceIn(0f, 1f), Modifier.padding(top = ZeroTheme.tokens.spacing.sectionGap))
    }
}

/** The essential apps: picked ones first, dimmed when not picked, like onboarding's favorites (G5, H11). */
@Composable
fun EssentialAppsScreen(
    state: LockSettingsState,
    onQueryChange: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val tokens = ZeroTheme.tokens
    val picked = state.settings.essentialApps.toSet()
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(SettingsTestTags.LOCK_APPS)) {
        BackTitle(stringResource(R.string.lock_settings_essential), onBack)
        BodyText(stringResource(R.string.lock_essential_phone_note), muted = true, small = true)
        SearchField(state.query, autoOpenKeyboard = false, onQueryChange = onQueryChange, onGo = {})
        HorizontalDivider(color = tokens.colors.divider)
        LazyColumn {
            items(state.apps, key = { it.key.asString() }) { app: AppEntry ->
                val key = app.key.asString()
                val on = key in picked
                MinimalListItem(
                    text = app.displayName(),
                    onClick = { onToggle(key, !on) },
                    muted = !on,
                    trailing = if (on) stringResource(R.string.common_on) else null,
                    large = true
                )
            }
        }
    }
}

/** Asks for the phrase before a change that weakens the lock (F19.6); shows it, like the unlock page (D-062). */
@Composable
private fun PhraseDialog(phrase: String, wrong: Boolean, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZeroTheme.tokens.colors.background,
        title = { Text(stringResource(R.string.lock_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ZeroTheme.tokens.spacing.sectionGap)) {
                Text(stringResource(R.string.lock_type_phrase, phrase), color = ZeroTheme.tokens.colors.text)
                PhraseInput(typed, { typed = it }, stringResource(R.string.lock_phrase_field))
                if (wrong) Text(stringResource(R.string.lock_confirm_wrong), color = ZeroTheme.tokens.colors.textMuted)
            }
        },
        confirmButton = {
            TextAction(stringResource(R.string.common_continue), { onConfirm(typed) }, compact = true, enabled = typed.isNotEmpty())
        },
        dismissButton = { TextAction(stringResource(R.string.common_cancel), onDismiss, compact = true) }
    )
}

/** Wireframe L2: a new phrase of 8–40 characters. */
@Composable
private fun NewPhraseDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var typed by remember { mutableStateOf(current) }
    val valid = typed.length in ScreenTimeLockSettingsViewModel.PHRASE_LENGTH && typed != current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZeroTheme.tokens.colors.background,
        title = { Text(stringResource(R.string.lock_settings_phrase)) },
        text = {
            PhraseInput(typed, {
                typed = it.take(ScreenTimeLockSettingsViewModel.PHRASE_LENGTH.last)
            }, stringResource(R.string.lock_new_phrase))
        },
        confirmButton = { TextAction(stringResource(R.string.common_save), { onSave(typed) }, compact = true, enabled = valid) },
        dismissButton = { TextAction(stringResource(R.string.common_cancel), onDismiss, compact = true) }
    )
}

@Composable
private fun PhraseInput(value: String, onChange: (String) -> Unit, label: String) {
    val tokens = ZeroTheme.tokens
    val style = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.text, fontSize = tokens.textSizes.body)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(tokens.colors.text),
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
        modifier = Modifier.fillMaxWidth().padding(vertical = tokens.spacing.listItemVertical)
            .semantics { contentDescription = label }.testTag(SettingsTestTags.LOCK_PHRASE),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) Text(label, style = style.copy(color = tokens.colors.textMuted))
                field()
            }
        }
    )
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val STEP_DOWN = "‹"
private const val STEP_UP = "›"
