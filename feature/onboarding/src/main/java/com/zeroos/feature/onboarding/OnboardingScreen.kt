package com.zeroos.feature.onboarding

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.DotMatrixIcon
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.displayName

/**
 * Onboarding with its ViewModel and the two system dialogs it may open (F14.2, F14.6). Lint's BatteryLife
 * check is about Play Store policy; ZeroOS is sideloaded (D-003) and F14.6 asks for this dialog.
 */
@SuppressLint("BatteryLife")
@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.page) { viewModel.onPageShown(state.page) }
    // Coming back from Android's dialogs or settings: check again what changed.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshSystemStatus()
        onPauseOrDispose {}
    }
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshSystemStatus()
        viewModel.onDefaultHomeResult(it.resultCode == Activity.RESULT_OK)
    }
    fun open(intent: Intent, fallback: Intent? = null) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            viewModel.onOpenFailed(e)
            fallback?.let { open(it) }
        }
    }
    OnboardingScreen(
        state = state,
        actions = OnboardingActions(
            onNext = viewModel::next,
            onBack = viewModel::back,
            onSkip = viewModel::skip,
            onToggle = viewModel::toggle,
            onSetDefault = {
                val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) homeRoleRequest(context) else null
                if (request != null) roleRequest.launch(request) else open(Intent(Settings.ACTION_HOME_SETTINGS))
            },
            onTurnOffBatteryOptimization = {
                open(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri()),
                    fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                )
            }
        )
    )
}

/** Android's "default home app" dialog (F14.2), or null where the role isn't available. */
@RequiresApi(Build.VERSION_CODES.Q)
private fun homeRoleRequest(context: Context): Intent? {
    val roles = context.getSystemService(RoleManager::class.java)
    return if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) roles.createRequestRoleIntent(RoleManager.ROLE_HOME) else null
}

data class OnboardingActions(
    val onNext: () -> Unit,
    val onBack: () -> Unit,
    val onSkip: () -> Unit,
    val onToggle: (AppEntry) -> Unit,
    val onSetDefault: () -> Unit,
    val onTurnOffBatteryOptimization: () -> Unit
)

/** Wireframes G1–G6. */
@Composable
fun OnboardingScreen(state: OnboardingState, actions: OnboardingActions) {
    val tokens = ZeroTheme.tokens
    // Back steps through pages; on the first page it behaves as on home (MainActivity).
    BackHandler(enabled = state.page != OnboardingPage.entries.first(), onBack = actions.onBack)
    Column(
        Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(OnboardingTestTags.ROOT)
    ) {
        if (state.page != OnboardingPage.BACKGROUND) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextAction(stringResource(R.string.onboarding_skip), actions.onSkip, muted = true)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth()) {
            when (state.page) {
                OnboardingPage.INTRO_1, OnboardingPage.INTRO_2, OnboardingPage.INTRO_3 -> IntroPage(state.page)
                OnboardingPage.DEFAULT_HOME -> DefaultHomePage(state.isDefaultHome)
                OnboardingPage.FAVORITES -> FavoritesPage(state, actions.onToggle)
                OnboardingPage.BACKGROUND -> BackgroundPage(state.batteryOptimized, actions.onTurnOffBatteryOptimization)
            }
        }
        BottomButtons(state, actions)
    }
}

@Composable
private fun BottomButtons(state: OnboardingState, actions: OnboardingActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        when {
            state.page == OnboardingPage.DEFAULT_HOME && !state.isDefaultHome -> {
                TextAction(stringResource(R.string.onboarding_set_default), actions.onSetDefault)
                TextAction(stringResource(R.string.onboarding_later), actions.onNext, muted = true)
            }

            state.page == OnboardingPage.BACKGROUND -> TextAction(stringResource(R.string.common_done), actions.onNext)

            else -> TextAction(stringResource(R.string.common_next), actions.onNext)
        }
    }
}

@Composable
private fun ColumnScope.IntroPage(page: OnboardingPage) {
    val tokens = ZeroTheme.tokens
    PageDots(page.ordinal, OnboardingPage.INTRO_PAGES)
    Spacer(Modifier.weight(1f))
    Text(
        stringResource(
            when (page) {
                OnboardingPage.INTRO_1 -> R.string.onboarding_intro_1
                OnboardingPage.INTRO_2 -> R.string.onboarding_intro_2
                else -> R.string.onboarding_intro_3
            }
        ),
        color = tokens.colors.text,
        fontSize = tokens.textSizes.title,
        modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter)
    )
    Spacer(Modifier.weight(2f))
}

/** "●○○" drawn as dots: Doto has neither ● nor ○. */
@Composable
private fun PageDots(current: Int, count: Int) {
    val tokens = ZeroTheme.tokens
    val description = stringResource(R.string.onboarding_page, current + 1, count)
    Row(
        Modifier.padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.sectionGap)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
    ) {
        repeat(count) {
            DotMatrixIcon(
                pattern = SINGLE_DOT,
                height = tokens.spacing.sectionGap,
                contentDescription = if (it == 0) description else "",
                color = if (it == current) tokens.colors.text else tokens.colors.divider
            )
        }
    }
}

@Composable
private fun DefaultHomePage(isDefaultHome: Boolean) {
    if (isDefaultHome) {
        ScreenTitle(stringResource(R.string.onboarding_default_done))
        BodyText(stringResource(R.string.onboarding_default_body))
    } else {
        ScreenTitle(stringResource(R.string.onboarding_default_title))
        BodyText(stringResource(R.string.onboarding_default_body))
        BodyText(stringResource(R.string.onboarding_default_fallback), muted = true)
    }
}

@Composable
private fun FavoritesPage(state: OnboardingState, onToggle: (AppEntry) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScreenTitle(stringResource(R.string.onboarding_favorites_title), Modifier.weight(1f))
        BodyText(stringResource(R.string.favorites_count, state.selected.size, state.maxFavorites), muted = true)
    }
    LazyColumn(Modifier.testTag(OnboardingTestTags.FAVORITES)) {
        items(state.apps, key = { it.key.asString() }) { app ->
            val position = state.selected.indexOf(app.key.asString())
            val checked = position >= 0
            // Doto has no ☑/☐: a picked app shows its place on home instead, others are dimmed.
            MinimalListItem(
                text = app.displayName(),
                onClick = { onToggle(app) },
                muted = !checked,
                trailing = if (checked) (position + 1).toString() else null,
                large = true,
                modifier = Modifier.semantics {
                    role = Role.Checkbox
                    toggleableState = ToggleableState(checked)
                }
            )
        }
    }
}

@Composable
private fun BackgroundPage(batteryOptimized: Boolean, onTurnOff: () -> Unit) {
    ScreenTitle(stringResource(R.string.onboarding_background_title))
    BodyText(stringResource(R.string.onboarding_background_body))
    BodyText(
        stringResource(
            R.string.settings_label_value,
            stringResource(R.string.onboarding_battery_optimization),
            stringResource(if (batteryOptimized) R.string.common_on else R.string.common_off)
        )
    )
    if (batteryOptimized) TextAction(stringResource(R.string.onboarding_battery_turn_off), onTurnOff)
    BodyText(stringResource(R.string.onboarding_oem_tip), muted = true)
}

private val SINGLE_DOT = listOf("●")

object OnboardingTestTags {
    const val ROOT = "onboarding"
    const val FAVORITES = "onboarding_favorites"
}
