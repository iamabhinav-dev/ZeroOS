package com.zeroos.feature.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Alignment.Companion.Start
import androidx.compose.ui.Alignment.Horizontal
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.Alignment
import com.zeroos.core.resources.R
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ZeroTheme

/** Optional home lines that later phases fill in (wireframe A1 ④ ⑤ ⑥ ⑦). Empty slots take no space. */
data class HomeSlots(
    val stats: (@Composable () -> Unit)? = null,
    val focus: (@Composable () -> Unit)? = null,
    val calendar: (@Composable () -> Unit)? = null,
    val todo: (@Composable () -> Unit)? = null
)

/** Home screen with its ViewModel (Step 2.3). */
@Composable
fun HomeRoute(onSwipeUp: () -> Unit, onLongPress: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun open(name: String, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            viewModel.onShortcutFailed(name, e)
        }
    }
    HomeScreen(
        state = state,
        onSwipeUp = onSwipeUp,
        onLongPress = onLongPress,
        onFavoriteClick = viewModel::launch,
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
    onSwipeUp: () -> Unit,
    onLongPress: () -> Unit,
    onFavoriteClick: (AppEntry) -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
    slots: HomeSlots = HomeSlots()
) {
    val tokens = ZeroTheme.tokens
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    val format = remember(locale, is24Hour) {
        ClockFormat(locale, is24Hour, DateFormat.getBestDateTimePattern(locale, ClockFormat.DATE_SKELETON))
    }
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
            .background(tokens.colors.background)
            .testTag(HomeTestTags.ROOT)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
            .pointerInput(minDistance, minVelocity) {
                val velocity = VelocityTracker()
                var dragY = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        dragY = 0f
                        velocity.resetTracking()
                    },
                    onDragEnd = {
                        if (isSwipeUp(dragY, velocity.calculateVelocity().y, minDistance, minVelocity)) onSwipeUp()
                    }
                ) { change, dragAmount ->
                    dragY += dragAmount
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

        Column(line.homeFavorites().testTag(HomeTestTags.FAVORITES), horizontalAlignment = horizontal) {
            state.favorites.forEach { app ->
                MinimalListItem(
                    text = app.label,
                    onClick = { onFavoriteClick(app) },
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
        Text(
            text = format.time(state.time),
            color = tokens.colors.text,
            fontSize = tokens.textSizes.clock,
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
        Text(
            text = state.batteryPercent?.let { stringResource(R.string.home_date_with_battery, date, it) } ?: date,
            color = tokens.colors.text,
            fontSize = tokens.textSizes.date,
            modifier = Modifier
                .clickable(onClickLabel = stringResource(R.string.home_open_calendar), role = Role.Button, onClick = onClick)
                .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
        )
    }
}

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
}
