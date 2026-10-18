package com.zeroos.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/** A row of dots, filled from the left up to [progress] (0–1): hold feedback (F8.12) and focus time passing (F9.4). */
@Composable
fun DotLine(progress: Float, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    val color = tokens.colors.text
    val faint = tokens.colors.divider
    Canvas(modifier.fillMaxWidth().height(tokens.spacing.sectionGap)) {
        val dot = size.height / 2
        val count = (size.width / (dot * 3)).toInt().coerceAtLeast(1)
        repeat(count) { i ->
            val filled = (i + 1f) / count <= progress
            drawCircle(if (filled) color else faint, dot / 2, Offset(dot + i * size.width / count, size.height / 2))
        }
    }
}
