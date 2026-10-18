package com.zeroos.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.HiddenAppsConfirmation
import com.zeroos.core.resources.R
import com.zeroos.core.ui.AppLabel
import com.zeroos.core.ui.BackTitle
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.displayName
import kotlinx.coroutines.delay

/** Hidden apps behind the optional confirmation or delay (F5.2, F5.3). */
@Composable
fun HiddenAppsRoute(onBack: () -> Unit, viewModel: HiddenAppsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked && state.confirmation != HiddenAppsConfirmation.OFF) {
        HiddenAppsGate(state.confirmation, onOpen = { unlocked = true }, onCancel = onBack)
    } else {
        HiddenAppsScreen(state.apps, onUnhide = viewModel::unhide, onBack = onBack)
    }
}

/** Wireframe E4. */
@Composable
fun HiddenAppsScreen(apps: List<AppEntry>, onUnhide: (AppEntry) -> Unit, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(HiddenAppsTestTags.LIST)) {
        item { BackTitle(stringResource(R.string.hidden_apps_title), onBack) }
        if (apps.isEmpty()) item { BodyText(stringResource(R.string.hidden_apps_empty), muted = true) }
        items(apps, key = { it.key.asString() }) { app ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(
                    min = tokens.spacing.minTouchTarget
                ).padding(start = tokens.spacing.screenGutter)
            ) {
                AppLabel(app.displayName(), Modifier.weight(1f))
                TextAction(stringResource(R.string.hidden_apps_unhide), onClick = { onUnhide(app) })
            }
        }
        item { BodyText(stringResource(R.string.hidden_apps_explanation), muted = true, small = true) }
    }
}

/** F5.3: either a confirm dialog or a 5-second countdown before hidden apps show. */
@Composable
fun HiddenAppsGate(mode: HiddenAppsConfirmation, onOpen: () -> Unit, onCancel: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onCancel)
    when (mode) {
        HiddenAppsConfirmation.OFF -> LaunchedEffect(Unit) { onOpen() }

        HiddenAppsConfirmation.CONFIRM -> AlertDialog(
            onDismissRequest = onCancel,
            containerColor = tokens.colors.background,
            title = { Text(stringResource(R.string.hidden_apps_confirm_title)) },
            confirmButton = { TextAction(stringResource(R.string.hidden_apps_confirm_show), onClick = onOpen, compact = true) },
            dismissButton = { TextAction(stringResource(R.string.common_cancel), onClick = onCancel, compact = true) }
        )

        HiddenAppsConfirmation.DELAY -> {
            var seconds by rememberSaveable { mutableIntStateOf(HIDDEN_APPS_DELAY_SECONDS) }
            LaunchedEffect(Unit) {
                while (seconds > 0) {
                    delay(ONE_SECOND_MS)
                    seconds--
                }
                onOpen()
            }
            Column(
                Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(HiddenAppsTestTags.DELAY),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                ScreenTitle(stringResource(R.string.hidden_apps_delay, seconds))
                TextAction(stringResource(R.string.common_cancel), onClick = onCancel)
            }
        }
    }
}

/** Appendix A: 5-second delay option for F5.3. */
internal const val HIDDEN_APPS_DELAY_SECONDS = 5
private const val ONE_SECOND_MS = 1_000L

object HiddenAppsTestTags {
    const val LIST = "hidden_apps"
    const val DELAY = "hidden_apps_delay"
}
