package com.zeroos.feature.wellbeing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.zeroos.core.resources.R
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * The short-video block screen (wireframe J3, F16.7, F16.9–F16.11, F16.17): the dot shutter closes over a play
 * symbol, then a pun from list A (or the custom [message]), "Go home" and, while uses are left, "Allow 5 minutes".
 * With [autoHomeSeconds] it goes home by itself (F16.10). Same layout as the other stop screens, two panes in landscape.
 */
@Composable
fun ShortFormScreen(
    pun: Int,
    message: String?,
    allowLeft: Int,
    autoHomeSeconds: Int?,
    onGoHome: () -> Unit,
    onAllow: () -> Unit
) {
    val home by rememberUpdatedState(onGoHome)
    if (autoHomeSeconds != null) {
        LaunchedEffect(autoHomeSeconds) {
            delay(autoHomeSeconds.seconds)
            home()
        }
    }
    Box(Modifier.fillMaxSize().background(ZeroTheme.tokens.colors.background).safeDrawingPadding().testTag(StopTestTags.SHORTS)) {
        StopLayout(
            art = { ClosingShutter(still = rememberReducedMotion()) },
            buttons = {
                TextAction(stringResource(R.string.wellbeing_go_home), onGoHome)
                if (allowLeft > 0) TextAction(stringResource(R.string.shorts_allow_button, allowLeft), onAllow, muted = true)
            }
        ) {
            Line(message?.takeIf { it.isNotBlank() } ?: punLine(R.array.shorts_puns, pun))
        }
    }
}

/**
 * A dotted play symbol with a dot shutter closing over it once, top to bottom (F16.17); closed and still when [still].
 * The shutter is drawn dimmer than the play symbol, so the closed picture still reads as "play, shut".
 */
@Composable
fun ClosingShutter(still: Boolean) {
    val tokens = ZeroTheme.tokens
    val closed = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(still) {
        if (!still) closed.animateTo(1f, tween(SHUTTER_MILLIS, easing = LinearEasing))
    }
    val description = stringResource(R.string.shorts_play)
    val color = tokens.colors.text
    val shutter = tokens.colors.textMuted
    val faint = tokens.colors.divider
    Canvas(Modifier.width(tokens.spacing.stopArtWidth).aspectRatio(1f).semantics { contentDescription = description }) {
        val step = size.width / GRID
        val dot = step / DOT_RATIO
        val shutterRows = (closed.value * GRID).toInt()
        for (row in 0 until GRID) {
            for (col in 0 until GRID) {
                val centre = Offset(step * (col + HALF), step * (row + HALF))
                when {
                    inPlaySymbol(row, col) -> drawCircle(color, dot, centre)
                    row < shutterRows -> drawCircle(shutter, dot * SHUTTER_DOT, centre)
                    else -> drawCircle(faint, dot / 2, centre)
                }
            }
        }
    }
}

/** A right-pointing triangle in the middle of the grid. */
private fun inPlaySymbol(row: Int, col: Int): Boolean {
    val left = GRID / 2 - PLAY_HALF
    val dx = col - left
    return dx in 0..(PLAY_HALF * 2) && abs(row - GRID / 2) <= (PLAY_HALF * 2 - dx) / 2
}

private const val GRID = 11
private const val PLAY_HALF = 2
private const val HALF = 0.5f
private const val DOT_RATIO = 3f
private const val SHUTTER_MILLIS = 900
private const val SHUTTER_DOT = 0.8f
