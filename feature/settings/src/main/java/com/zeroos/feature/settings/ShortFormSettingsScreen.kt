package com.zeroos.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.system.SystemActions
import com.zeroos.core.logging.redacted
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.CUSTOM_MESSAGE_MAX
import com.zeroos.core.model.settings.INSTAGRAM_PACKAGE
import com.zeroos.core.model.settings.ShortFormMode
import com.zeroos.core.model.settings.ShortFormSettings
import com.zeroos.core.model.settings.YOUTUBE_PACKAGE
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.PermissionRequest
import com.zeroos.core.ui.SettingRow
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ToggleRow
import com.zeroos.core.ui.ZeroTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Short videos page (J2): settings plus whether Accessibility is on and actually running (F16.15). */
data class ShortFormUiState(
    val settings: ShortFormSettings = ShortFormSettings(),
    val accessibilityOn: Boolean = false,
    val running: Boolean = false
)

/** Rows that step through their options (Appendix A ranges). */
enum class ShortFormCycle(val logName: String, val next: (ShortFormSettings) -> ShortFormSettings) {
    MODE("mode", { it.copy(mode = ShortFormMode.entries[(it.mode.ordinal + 1) % ShortFormMode.entries.size]) }),
    ALLOWANCE("allowanceMinutes", { it.copy(allowanceMinutes = ALLOWANCE_STEPS.after(it.allowanceMinutes)) }),
    MESSAGE("useCustomMessage", { it.copy(useCustomMessage = !it.useCustomMessage) }),
    AUTO_HOME("autoHomeSeconds", { it.copy(autoHomeSeconds = AUTO_HOME_STEPS.getOrNull(AUTO_HOME_STEPS.indexOf(it.autoHomeSeconds) + 1)) }),
    ALLOW_USES("allowUsesPerDay", { it.copy(allowUsesPerDay = (it.allowUsesPerDay + 1) % (MAX_ALLOW_USES + 1)) })
}

/** Settings → Wellbeing → Short videos (F16.1–F16.4, F16.7–F16.11, F16.15). Changes are logged (F17.7). */
@HiltViewModel
class ShortFormSettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    /** For the Accessibility explanation (F16.2). */
    val permissions: SpecialPermissions,
    systemActions: SystemActions
) : ViewModel() {
    private val accessibility = MutableStateFlow(false)

    val state: StateFlow<ShortFormUiState> = combine(settings.settings, accessibility, systemActions.running) { s, on, running ->
        ShortFormUiState(s.shortForm, on, running)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ShortFormUiState())

    fun refreshAccessibility() {
        accessibility.value = permissions.isGranted(SpecialPermission.ACCESSIBILITY)
    }

    fun setEnabled(on: Boolean) = update("enabled=$on") { it.copy(enabled = on) }

    /** F16.4: block one app's short videos or not. */
    fun setApp(packageName: String, on: Boolean) =
        update("app $packageName=$on") { it.copy(blockedApps = if (on) it.blockedApps + packageName else it.blockedApps - packageName) }

    fun cycle(option: ShortFormCycle) = update(option.logName, option.next)

    /** F16.9: up to 120 characters; logged only as its length (F17.8). */
    fun setCustomMessage(text: String) {
        val message = text.take(CUSTOM_MESSAGE_MAX)
        update("customMessage=${redacted(message)}") { it.copy(customMessage = message, useCustomMessage = message.isNotBlank()) }
    }

    /** Result of the Accessibility explanation; granted turns the blocker on (F16.1, F16.2). */
    fun onAccessibilityResult(granted: Boolean) {
        permissions.record(SpecialPermission.ACCESSIBILITY, granted)
        refreshAccessibility()
        if (granted) setEnabled(true)
    }

    private fun update(change: String, transform: (ShortFormSettings) -> ShortFormSettings) {
        viewModelScope.launch { settings.update("shortForm.$change") { it.copy(shortForm = transform(it.shortForm)) } }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** F16.8: 0–120 min, starting at the 15 min default. */
private val ALLOWANCE_STEPS = listOf(15, 20, 30, 45, 60, 90, 120, 0, 5, 10)

/** F16.10: off or 3–10 s. */
private val AUTO_HOME_STEPS: List<Int?> = listOf(null) + (3..10).toList()

/** F16.11: 0–5 uses a day. */
private const val MAX_ALLOW_USES = 5

private fun List<Int>.after(current: Int) = getOrElse(indexOf(current) + 1) { first() }

@Composable
fun ShortFormSettingsRoute(onBack: () -> Unit, viewModel: ShortFormSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var explaining by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshAccessibility()
        onPauseOrDispose {}
    }
    if (explaining) {
        PermissionRequest(
            permission = SpecialPermission.ACCESSIBILITY,
            isGranted = { viewModel.permissions.isGranted(SpecialPermission.ACCESSIBILITY) },
            openSettings = { viewModel.permissions.openSettings(SpecialPermission.ACCESSIBILITY) },
            openAppInfo = viewModel.permissions::openAppInfo,
            onResult = {
                explaining = false
                viewModel.onAccessibilityResult(it)
            }
        )
        return
    }
    ShortFormSettingsScreen(
        state = state,
        actions = ShortFormActions(
            onEnabled = { on -> if (on && !state.accessibilityOn) explaining = true else viewModel.setEnabled(on) },
            onApp = viewModel::setApp,
            onCycle = viewModel::cycle,
            onEditMessage = { editing = true },
            onTurnOnAccessibility = { explaining = true }
        ),
        onBack = onBack
    )
    if (editing) {
        CustomMessageDialog(
            initial = state.settings.customMessage,
            onSave = {
                editing = false
                viewModel.setCustomMessage(it)
            },
            onCancel = { editing = false }
        )
    }
}

data class ShortFormActions(
    val onEnabled: (Boolean) -> Unit,
    val onApp: (String, Boolean) -> Unit,
    val onCycle: (ShortFormCycle) -> Unit,
    val onEditMessage: () -> Unit,
    val onTurnOnAccessibility: () -> Unit
)

/** Wireframe J2; the rows below the switch show only while the blocker is on. */
@Composable
fun ShortFormSettingsScreen(state: ShortFormUiState, actions: ShortFormActions, onBack: () -> Unit) {
    val s = state.settings
    SettingsPageScaffold(stringResource(R.string.shorts_title), SettingsTestTags.SHORT_VIDEOS, onBack) {
        item { ToggleRow(stringResource(R.string.shorts_enable), s.enabled, actions.onEnabled) }
        if (!s.enabled) return@SettingsPageScaffold
        item {
            ToggleRow(stringResource(R.string.shorts_youtube), YOUTUBE_PACKAGE in s.blockedApps, { actions.onApp(YOUTUBE_PACKAGE, it) })
        }
        item {
            ToggleRow(stringResource(R.string.shorts_instagram), INSTAGRAM_PACKAGE in s.blockedApps, {
                actions.onApp(INSTAGRAM_PACKAGE, it)
            })
        }
        item {
            SettingRow(stringResource(R.string.shorts_mode), stringResource(modeName(s.mode)), { actions.onCycle(ShortFormCycle.MODE) })
        }
        if (s.mode == ShortFormMode.LIMIT) {
            item {
                SettingRow(
                    stringResource(R.string.shorts_allowance),
                    stringResource(R.string.wellbeing_minutes, s.allowanceMinutes),
                    { actions.onCycle(ShortFormCycle.ALLOWANCE) }
                )
            }
        }
        item {
            SettingRow(
                stringResource(R.string.shorts_message),
                stringResource(if (s.useCustomMessage) R.string.shorts_message_custom else R.string.shorts_message_pun),
                { actions.onCycle(ShortFormCycle.MESSAGE) }
            )
        }
        if (s.useCustomMessage) item { NavRow(R.string.shorts_custom, actions.onEditMessage) }
        item {
            SettingRow(
                stringResource(R.string.shorts_auto_home),
                s.autoHomeSeconds?.let { stringResource(R.string.wellbeing_seconds, it) } ?: stringResource(R.string.common_off),
                { actions.onCycle(ShortFormCycle.AUTO_HOME) }
            )
        }
        item {
            SettingRow(
                stringResource(R.string.shorts_allow),
                stringResource(R.string.wellbeing_extra_value, s.allowUsesPerDay),
                { actions.onCycle(ShortFormCycle.ALLOW_USES) }
            )
        }
        // F16.15: Accessibility switched off, or on but stopped by the phone.
        when {
            !state.accessibilityOn -> item {
                TextAction(stringResource(R.string.gestures_accessibility_off), actions.onTurnOnAccessibility, muted = true)
            }

            !state.running -> item { BodyText(stringResource(R.string.gestures_accessibility_stopped), muted = true, small = true) }
        }
    }
}

private fun modeName(mode: ShortFormMode) = when (mode) {
    ShortFormMode.BLOCK -> R.string.shorts_mode_block
    ShortFormMode.LIMIT -> R.string.shorts_mode_limit
}

/** Wireframe J6: up to 120 characters with a "left" counter; the text is never logged (F17.8). */
@Composable
fun CustomMessageDialog(initial: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    val tokens = ZeroTheme.tokens
    var text by remember { mutableStateOf(initial) }
    val style = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.text, fontSize = tokens.textSizes.body)
    val hint = stringResource(R.string.shorts_custom_hint)
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = tokens.colors.background,
        title = { Text(stringResource(R.string.shorts_custom)) },
        text = {
            Column {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.take(CUSTOM_MESSAGE_MAX) },
                    textStyle = style,
                    cursorBrush = SolidColor(tokens.colors.text),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }.testTag(SettingsTestTags.SHORT_MESSAGE),
                    decorationBox = { field ->
                        Box {
                            if (text.isEmpty()) Text(hint, style = style.copy(color = tokens.colors.textMuted))
                            field()
                        }
                    }
                )
                val left = CUSTOM_MESSAGE_MAX - text.length
                Text(
                    pluralStringResource(R.plurals.shorts_chars_left, left, left),
                    color = tokens.colors.textMuted,
                    fontSize = tokens.textSizes.small
                )
            }
        },
        confirmButton = { TextAction(stringResource(R.string.common_save), { onSave(text) }, compact = true) },
        dismissButton = { TextAction(stringResource(R.string.common_cancel), onCancel, compact = true) }
    )
}
