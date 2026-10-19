package com.zeroos.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.zeroos.core.ui.ZeroTheme
import kotlin.math.ceil

/**
 * Columns of dots, one per value, height proportional to the largest (F10.5, H12). The [highlight] column (today) is
 * solid; the others are hollow when [hollowOthers]. Empty values show nothing, so the chart reads as "none". The
 * [description] states the values for screen readers (F10.13).
 */
@Composable
internal fun DotColumns(
    values: List<Long>,
    rows: Int,
    height: Dp,
    description: String,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
    hollowOthers: Boolean = false
) {
    val tokens = ZeroTheme.tokens
    val color = tokens.colors.text
    val faint = tokens.colors.textMuted
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1
    Canvas(modifier.fillMaxWidth().height(height).semantics { contentDescription = description }) {
        val step = size.width / values.size
        val dot = minOf(step, size.height / rows) / DOT_SPACING
        values.forEachIndexed { i, value ->
            val filled = ceil(value.toDouble() / max * rows).toInt().coerceIn(0, rows)
            val x = step * (i + HALF)
            val solid = highlight == null || i == highlight || !hollowOthers
            repeat(filled) { r ->
                val y = size.height - (size.height / rows) * (r + HALF)
                if (solid) {
                    drawCircle(if (highlight == null || i == highlight) color else faint, dot, Offset(x, y))
                } else {
                    drawCircle(color, dot, Offset(x, y), style = Stroke(width = dot / HOLLOW_STROKE))
                }
            }
        }
    }
}

/** A row of dots filled up to [fraction] of the width: top-app bars, longest = full width (H12). */
@Composable
internal fun DotBar(fraction: Float, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    val color = tokens.colors.text
    // Same dot size and spacing as the focus dot line, so bars read as rows of small dots.
    Canvas(modifier.fillMaxWidth().height(tokens.spacing.sectionGap)) {
        val dot = size.height / 2
        val count = (size.width / (dot * DOT_SPACING)).toInt().coerceAtLeast(1)
        val shown = ceil(fraction.coerceIn(0f, 1f) * count).toInt().coerceAtLeast(if (fraction > 0) 1 else 0)
        repeat(shown) { i -> drawCircle(color, dot / 2, Offset(dot + i * size.width / count, size.height / 2)) }
    }
}

private const val HALF = 0.5f
private const val DOT_SPACING = 3f
private const val HOLLOW_STROKE = 3f
