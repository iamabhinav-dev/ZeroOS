package com.zeroos.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.permissions.SpecialPermissions
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.model.settings.FocusSettings
import com.zeroos.core.resources.R
import com.zeroos.core.ui.FocusMark
import com.zeroos.core.ui.PermissionRequest
import com.zeroos.core.ui.ToggleRow
import com.zeroos.core.ui.ZeroTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings → Wellbeing → Focus (F9.1, F9.7, F9.8, F9.10). Changes are logged by [SettingsRepository] (F17.7). */
@HiltViewModel
class FocusSettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    /** For the Do Not Disturb explanation (F9.7, Step 4.1). */
    val permissions: SpecialPermissions
) : ViewModel() {
    val state: StateFlow<FocusSettings> = settings.settings.map { it.focus }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FocusSettings())

    fun setShowOnHome(on: Boolean) = update("showOnHome=$on") { it.copy(showOnHome = on) }

    fun setIcon(icon: FocusIcon) = update("icon=$icon") { it.copy(icon = icon) }

    fun setDoNotDisturb(on: Boolean) = update("doNotDisturb=$on") { it.copy(doNotDisturb = on) }

    /** Result of the Do Not Disturb explanation; granted also turns the setting on (F9.7). */
    fun onDndResult(granted: Boolean) {
        permissions.record(SpecialPermission.DO_NOT_DISTURB, granted)
        if (granted) setDoNotDisturb(true)
    }

    private fun update(change: String, transform: (FocusSettings) -> FocusSettings) {
        viewModelScope.launch { settings.update("focus.$change") { it.copy(focus = transform(it.focus)) } }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** H10 and H10b, with the Do Not Disturb explanation when turning it on without access (F9.7). */
@Composable
fun FocusSettingsRoute(
    showIcons: Boolean,
    onOpenIcons: () -> Unit,
    onStartFocus: () -> Unit,
    onBack: () -> Unit,
    viewModel: FocusSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var explaining by rememberSaveable { mutableStateOf(false) }
    var hasAccess by rememberSaveable { mutableStateOf(viewModel.permissions.isGranted(SpecialPermission.DO_NOT_DISTURB)) }
    LifecycleResumeEffect(Unit) {
        hasAccess = viewModel.permissions.isGranted(SpecialPermission.DO_NOT_DISTURB)
        onPauseOrDispose {}
    }
    if (explaining) {
        PermissionRequest(
            permission = SpecialPermission.DO_NOT_DISTURB,
            isGranted = { viewModel.permissions.isGranted(SpecialPermission.DO_NOT_DISTURB) },
            openSettings = { viewModel.permissions.openSettings(SpecialPermission.DO_NOT_DISTURB) },
            openAppInfo = viewModel.permissions::openAppInfo,
            onResult = {
                explaining = false
                viewModel.onDndResult(it)
            }
        )
        return
    }
    if (showIcons) {
        FocusIconScreen(state.icon, viewModel::setIcon, onBack)
        return
    }
    FocusSettingsScreen(
        state = state,
        actions = FocusSettingsActions(
            onShowOnHome = viewModel::setShowOnHome,
            onOpenIcons = onOpenIcons,
            onStartFocus = onStartFocus,
            onDoNotDisturb = { on -> if (on && !hasAccess) explaining = true else viewModel.setDoNotDisturb(on) }
        ),
        onBack = onBack
    )
}

data class FocusSettingsActions(
    val onShowOnHome: (Boolean) -> Unit,
    val onOpenIcons: () -> Unit,
    val onStartFocus: () -> Unit,
    val onDoNotDisturb: (Boolean) -> Unit
)

/** Wireframe H10. */
@Composable
fun FocusSettingsScreen(state: FocusSettings, actions: FocusSettingsActions, onBack: () -> Unit) {
    SettingsPageScaffold(stringResource(R.string.focus_title), SettingsTestTags.FOCUS, onBack) {
        item { ToggleRow(stringResource(R.string.focus_show_on_home), state.showOnHome, actions.onShowOnHome) }
        item { FocusIconRow(state.icon, actions.onOpenIcons) }
        item { NavRow(R.string.focus_start, actions.onStartFocus) }
        item { ToggleRow(stringResource(R.string.focus_dnd), state.doNotDisturb, actions.onDoNotDisturb) }
    }
}

/** "Focus icon (F) ›": the row shows a small preview of the chosen icon (H10, F9.9d). */
@Composable
private fun FocusIconRow(icon: FocusIcon, onClick: () -> Unit) {
    val tokens = ZeroTheme.tokens
    Row(
        Modifier.fillMaxWidth().heightIn(min = tokens.spacing.minTouchTarget).clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
    ) {
        Text(
            stringResource(R.string.focus_icon),
            color = tokens.colors.text,
            fontSize = tokens.textSizes.body,
            modifier = Modifier.weight(1f)
        )
        FocusMark(icon, tokens.spacing.focusMarkSmall, stringResource(iconName(icon)))
        Text(CHEVRON, color = tokens.colors.textMuted, fontSize = tokens.textSizes.body)
    }
}

/** Wireframe H10b: the four designs, the chosen one outlined (F9.8). */
@Composable
fun FocusIconScreen(selected: FocusIcon, onSelect: (FocusIcon) -> Unit, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    SettingsPageScaffold(stringResource(R.string.focus_icon), SettingsTestTags.FOCUS_ICON, onBack) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.screenGutter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FocusIcon.entries.forEach { icon ->
                    val name = stringResource(iconName(icon))
                    Column(
                        Modifier
                            .then(if (icon == selected) Modifier.border(tokens.spacing.selectedOutline, tokens.colors.text) else Modifier)
                            .clickable { onSelect(icon) }
                            .semantics {
                                role = Role.RadioButton
                                this.selected = icon == selected
                            }
                            .padding(tokens.spacing.sectionGap),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FocusMark(icon, tokens.spacing.thumbnailWidth / 2, name)
                        Text(name, color = tokens.colors.text, fontSize = tokens.textSizes.small)
                    }
                }
            }
        }
    }
}

private fun iconName(icon: FocusIcon) = when (icon) {
    FocusIcon.MIX -> R.string.focus_icon_mix
    FocusIcon.SPHERE -> R.string.focus_icon_sphere
    FocusIcon.RIM -> R.string.focus_icon_rim
    FocusIcon.FLOW -> R.string.focus_icon_flow
}
