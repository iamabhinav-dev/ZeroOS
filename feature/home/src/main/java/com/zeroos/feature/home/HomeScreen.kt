package com.zeroos.feature.home

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.ui.ZeroTheme
import java.time.ZonedDateTime

/** Home screen with its ViewModel (Step 1.3). */
@Composable
fun HomeRoute(onSwipeUp: () -> Unit, onLongPress: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val time by viewModel.time.collectAsStateWithLifecycle()
    HomeScreen(time = time, onSwipeUp = onSwipeUp, onLongPress = onLongPress)
}

/**
 * Clock and date on pure black; swipe up anywhere opens the app list (F1.1, F1.2, F4.1; wireframe A2).
 * Long-press on empty space opens Diagnostics in Phase 1 and Settings from Step 2.11 (F1.9).
 */
@Composable
fun HomeScreen(time: ZonedDateTime, onSwipeUp: () -> Unit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
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

    Column(
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
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.screenGutter)
    ) {
        Text(text = format.time(time), color = tokens.colors.text, fontSize = tokens.textSizes.clock)
        Text(text = format.date(time), color = tokens.colors.text, fontSize = tokens.textSizes.date)
    }
}

object HomeTestTags {
    const val ROOT = "home"
}
