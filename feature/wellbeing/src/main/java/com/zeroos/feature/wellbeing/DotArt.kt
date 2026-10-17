package com.zeroos.feature.wellbeing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.zeroos.core.resources.R
import com.zeroos.core.ui.ZeroTheme
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * Breathing dots (F8.15): a ring of dots around a centre dot that grows and shrinks once over the pause, so the
 * breath ends with the countdown. Drawn mid-breath and still when [still].
 */
@Composable
fun BreathingDots(seconds: Int, still: Boolean) {
    val tokens = ZeroTheme.tokens
    val progress = remember { Animatable(if (still) HALF else 0f) }
    LaunchedEffect(still) {
        if (!still) progress.animateTo(1f, tween(seconds * MILLIS_PER_SECOND, easing = LinearEasing))
    }
    val description = stringResource(R.string.wellbeing_breathing)
    val color = tokens.colors.text
    val faint = tokens.colors.textMuted
    Canvas(Modifier.width(tokens.spacing.stopArtWidth).aspectRatio(1f).semantics { contentDescription = description }) {
        val breath = sin(PI * progress.value).toFloat()
        val unit = size.minDimension / UNITS
        drawCircle(color, unit * (CENTRE_MIN + CENTRE_GROW * breath), center)
        val ring = unit * (RING_MIN + RING_GROW * breath)
        repeat(RING_DOTS) { i ->
            val angle = 2 * PI * i / RING_DOTS
            drawCircle(faint, unit * RING_DOT, center + Offset((ring * cos(angle)).toFloat(), (ring * sin(angle)).toFloat()))
        }
    }
}

/** A dot battery that drains to empty once (F8.15); shown empty and still when [still]. */
@Composable
fun DrainingBattery(still: Boolean) {
    val tokens = ZeroTheme.tokens
    val level = remember { Animatable(if (still) 0f else 1f) }
    LaunchedEffect(still) {
        if (!still) level.animateTo(0f, tween(DRAIN_MILLIS, easing = LinearEasing))
    }
    val description = stringResource(R.string.wellbeing_battery_empty)
    val color = tokens.colors.text
    val faint = tokens.colors.divider
    Canvas(Modifier.width(tokens.spacing.stopArtWidth).aspectRatio(2f).semantics { contentDescription = description }) {
        // Frame of small dots, CELLS big dots inside, and a two-dot terminal on the right.
        val step = size.width / (FRAME_COLUMNS + 1)
        val small = step / FRAME_DOT_RATIO
        val rows = (size.height / step).toInt()
        for (c in 0 until FRAME_COLUMNS) {
            drawCircle(color, small, Offset(step * (c + 0.5f), step * 0.5f))
            drawCircle(color, small, Offset(step * (c + 0.5f), step * (rows - 0.5f)))
        }
        for (r in 1 until rows - 1) {
            drawCircle(color, small, Offset(step * 0.5f, step * (r + 0.5f)))
            drawCircle(color, small, Offset(step * (FRAME_COLUMNS - 0.5f), step * (r + 0.5f)))
        }
        val middle = rows / 2f
        drawCircle(color, small, Offset(step * (FRAME_COLUMNS + 0.5f), step * (middle - 0.5f)))
        drawCircle(color, small, Offset(step * (FRAME_COLUMNS + 0.5f), step * (middle + 0.5f)))
        val lit = ceil(level.value * CELLS).toInt()
        val cellStep = step * (FRAME_COLUMNS - 2) / CELLS
        repeat(CELLS) { i ->
            val x = step + cellStep * (i + 0.5f)
            drawCircle(if (i < lit) color else faint, cellStep / CELL_DOT_RATIO, Offset(x, size.height / 2))
        }
    }
}

private const val HALF = 0.5f
private const val MILLIS_PER_SECOND = 1_000
private const val DRAIN_MILLIS = 2_000
private const val UNITS = 10f
private const val CENTRE_MIN = 0.8f
private const val CENTRE_GROW = 0.7f
private const val RING_MIN = 2.2f
private const val RING_GROW = 2.2f
private const val RING_DOT = 0.35f
private const val RING_DOTS = 8
private const val FRAME_COLUMNS = 13
private const val FRAME_DOT_RATIO = 4f
private const val CELLS = 5
private const val CELL_DOT_RATIO = 3f
