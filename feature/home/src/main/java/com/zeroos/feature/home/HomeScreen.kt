package com.zeroos.feature.home

import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Alignment.Companion.Start
import androidx.compose.ui.Alignment.Horizontal
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.resources.R
import com.zeroos.core.ui.AppMenu
import com.zeroos.core.ui.AppMenuActions
import com.zeroos.core.ui.AppMenuTarget
import com.zeroos.core.ui.ClockFormat
import com.zeroos.core.ui.ClockText
import com.zeroos.core.ui.DotIcons
import com.zeroos.core.ui.DotMatrixIcon
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.displayName

/** Optional home lines that later phases fill in (wireframe A1 ④ ⑤ ⑥ ⑦). Empty slots take no space. */
data class HomeSlots(
    val stats: (@Composable () -> Unit)? = null,
    val focus: (@Composable () -> Unit)? = null,
    val calendar: (@Composable () -> Unit)? = null,
    val todo: (@Composable () -> Unit)? = null
)

/** Home screen with its ViewModel (Step 2.3). */
@Composable
fun HomeRoute(onSwipeUp: () -> Unit, onSwipeDown: () -> Unit, onLongPress: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun open(name: String, intent: Intent) {
        startShortcut(context, intent)?.let { viewModel.onShortcutFailed(name, it) }
    }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    menuFor?.let { app ->
        AppMenu(
            target = AppMenuTarget(
                app.displayName(),
                isFavorite = true,
                isRenamed = app.key.asString() in state.renamedKeys,
                canUninstall = app.canUninstall
            ),
            actions = AppMenuActions(
                onToggleFavorite = { viewModel.removeFavorite(app) },
                onRename = { viewModel.rename(app, it) },
                onHide = { viewModel.hide(app) },
                onAppInfo = { viewModel.openAppInfo(app) },
                onUninstall = { viewModel.uninstall(app) }
            ),
            onDismiss = { menuFor = null }
        )
    }
    HomeScreen(
        state = state,
        onSwipe = { swipe ->
            when (swipe) {
                Swipe.UP -> onSwipeUp()
                Swipe.DOWN -> onSwipeDown()
                Swipe.LEFT -> state.swipeLeftApp?.let(viewModel::launch)
                Swipe.RIGHT -> state.swipeRightApp?.let(viewModel::launch)
            }
        },
        onLongPress = onLongPress,
        onFavoriteClick = viewModel::launch,
        onFavoriteLongClick = { menuFor = it },
        onClockClick = { open("alarms", Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
        onDateClick = { open("calendar", Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)) }
    )
}

/**
 * Home (F1, wireframes A1/A2): clock, date with battery, optional next alarm and later slots at the top;
 * favorites in the lower half, always fully visible; never scrolls (F1.6). Swipe up opens the app list (F4.1);
 * long-press on empty space opens the menu (F1.9).
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onSwipe: (Swipe) -> Unit,
    onLongPress: () -> Unit,
    onFavoriteClick: (AppEntry) -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteLongClick: (AppEntry) -> Unit = {},
    slots: HomeSlots = HomeSlots()
) {
    val tokens = ZeroTheme.tokens
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    val format = remember(locale, is24Hour) { ClockFormat.forDevice(context, locale) }
    val density = LocalDensity.current
    val minDistance = with(density) { tokens.gestures.swipeDistance.toPx() }
    val minVelocity = with(density) { tokens.gestures.swipeVelocity.toPx() }
    val horizontal = when (state.alignment) {
        Alignment.LEFT -> Start
        Alignment.CENTER -> CenterHorizontally
        Alignment.RIGHT -> End
    }
    val line = Modifier.fillMaxWidth()

    HomeLayout(
        modifier = modifier
            .fillMaxSize()
            .background(if (state.showWallpaper) tokens.colors.wallpaperScrim else tokens.colors.background)
            .testTag(HomeTestTags.ROOT)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
            .pointerInput(minDistance, minVelocity) {
                val velocity = VelocityTracker()
                var drag = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        drag = Offset.Zero
                        velocity.resetTracking()
                    },
                    onDragEnd = {
                        swipeOf(drag, velocity.calculateVelocity(), minDistance, minVelocity)?.let(onSwipe)
                    }
                ) { change, dragAmount ->
                    drag += dragAmount
                    velocity.addPosition(change.uptimeMillis, change.position)
                }
            }
            .safeDrawingPadding()
            .padding(vertical = tokens.spacing.screenGutter)
    ) {
        if (state.showClock) ClockLine(state, format, horizontal, onClockClick, line.homeSlot(HomeSlot.CLOCK))
        if (state.showDate) DateLine(state, format, horizontal, onDateClick, line.homeSlot(HomeSlot.DATE))
        state.nextAlarm?.let { AlarmLine(format.time(it), horizontal, line.homeSlot(HomeSlot.NEXT_ALARM)) }
        listOf(
            HomeSlot.STATS to slots.stats,
            HomeSlot.FOCUS to slots.focus,
            HomeSlot.CALENDAR to slots.calendar,
            HomeSlot.TODO to slots.todo
        ).forEach { (slot, content) ->
            content?.let { Column(line.homeSlot(slot), horizontalAlignment = horizontal) { it() } }
        }

        // Scrolls only when the favorites don't fit (many favorites, large text, landscape); otherwise swipes
        // on them still reach the home gestures.
        val favoritesScroll = rememberScrollState()
        Column(
            line.homeFavorites()
                .verticalScroll(favoritesScroll, enabled = favoritesScroll.maxValue > 0)
                .testTag(HomeTestTags.FAVORITES),
            horizontalAlignment = horizontal
        ) {
            state.favorites.forEach { app ->
                MinimalListItem(
                    text = app.displayName(),
                    onClick = { onFavoriteClick(app) },
                    onLongClick = { onFavoriteLongClick(app) },
                    large = true,
                    modifier = Modifier.fillMaxWidth(),
                    alignment = state.alignment
                )
            }
        }
    }
}

@Composable
private fun ClockLine(
    state: HomeUiState,
    format: ClockFormat,
    horizontal: Horizontal,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val tokens = ZeroTheme.tokens
    Column(modifier, horizontalAlignment = horizontal) {
        ClockText(
            text = format.time(state.time, state.showSeconds),
            modifier = Modifier
                .clickable(onClickLabel = stringResource(R.string.home_open_alarms), role = Role.Button, onClick = onClick)
                .padding(horizontal = tokens.spacing.screenGutter)
        )
    }
}

@Composable
private fun DateLine(
    state: HomeUiState,
    format: ClockFormat,
    horizontal: Horizontal,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val tokens = ZeroTheme.tokens
    val date = format.date(state.time)
    Column(modifier, horizontalAlignment = horizontal) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClickLabel = stringResource(R.string.home_open_calendar), role = Role.Button, onClick = onClick)
                .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
        ) {
            Text(
                text = state.batteryPercent?.let { stringResource(R.string.home_date_with_battery, date, it) } ?: date,
                color = tokens.colors.text,
                fontSize = tokens.textSizes.date
            )
            if (state.charging) {
                val height = with(LocalDensity.current) { (tokens.textSizes.date * BOLT_HEIGHT_OF_TEXT).toDp() }
                DotMatrixIcon(
                    pattern = DotIcons.Bolt,
                    height = height,
                    contentDescription = stringResource(R.string.home_charging),
                    modifier = Modifier.padding(start = tokens.spacing.listItemCompact).testTag(HomeTestTags.CHARGING)
                )
            }
        }
    }
}

/** The bolt matches Doto's capital-letter height, about 70% of the font size. */
private const val BOLT_HEIGHT_OF_TEXT = 0.7f

@Composable
private fun AlarmLine(time: String, horizontal: Horizontal, modifier: Modifier) {
    val tokens = ZeroTheme.tokens
    Column(modifier, horizontalAlignment = horizontal) {
        Text(
            text = stringResource(R.string.home_next_alarm, time),
            color = tokens.colors.textMuted,
            fontSize = tokens.textSizes.small,
            modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter)
        )
    }
}

object HomeTestTags {
    const val ROOT = "home"
    const val FAVORITES = "home_favorites"
    const val CHARGING = "home_charging"
}
