package com.zeroos.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * Draws a small icon as a grid of round dots, matching Doto's dot-matrix letters (D-020, D-031).
 * [pattern] rows use '●' (or any non-space, non-'.' character) for a dot and '.' or ' ' for empty.
 * Used where Doto has no glyph, e.g. the charging bolt, so no colored emoji fallback appears.
 */
@Composable
fun DotMatrixIcon(
    pattern: List<String>,
    height: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = ZeroTheme.tokens.colors.text
) {
    val rows = pattern.size
    val cols = pattern.maxOf { it.length }
    Canvas(
        modifier = modifier
            .height(height)
            .width(height * cols / rows)
            .semantics { this.contentDescription = contentDescription }
    ) {
        val cell = size.height / rows
        val radius = cell * DOT_FILL / 2
        pattern.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch ->
                if (ch != '.' && ch != ' ') {
                    drawCircle(color, radius, Offset(col * cell + cell / 2, row * cell + cell / 2))
                }
            }
        }
    }
}

/** Share of each grid cell filled by its dot, close to Doto's dot size. */
private const val DOT_FILL = 0.8f

/** Dot-matrix icons on Doto's 5 × 7 letter grid. */
object DotIcons {
    /** Drag handle for reordering (F3.2): three dotted lines. Doto has no "≡". */
    val DragHandle = listOf(
        ".....",
        "●●●●●",
        ".....",
        "●●●●●",
        ".....",
        "●●●●●",
        "....."
    )

    /** Back arrow (F6.8 wallpaper preview). Doto has no "←". */
    val Back = listOf(
        ".....",
        "..●..",
        ".●...",
        "●●●●●",
        ".●...",
        "..●..",
        "....."
    )

    /** Lightning bolt shown while charging (F1.3). */
    val Bolt = listOf(
        "...●.",
        "..●..",
        ".●...",
        "●●●●●",
        "...●.",
        "..●..",
        ".●..."
    )
}
