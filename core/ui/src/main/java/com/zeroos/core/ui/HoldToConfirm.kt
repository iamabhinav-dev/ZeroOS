package com.zeroos.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import kotlin.time.Duration
import kotlinx.coroutines.launch

/**
 * A button that only acts when held for [duration] (F8.12, later F9.5): a dotted line fills while it's held and
 * empties if let go early. Screen readers get a plain action instead of the hold.
 */
@Composable
fun HoldToConfirm(
    text: String,
    duration: Duration,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    val tokens = ZeroTheme.tokens
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val confirm by rememberUpdatedState(onConfirm)
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.minTouchTarget)
            .semantics(mergeDescendants = true) { onClick(label = text) { confirm().let { true } } }
            .pointerInput(duration) {
                awaitEachGesture {
                    awaitFirstDown()
                    val fill = scope.launch {
                        progress.animateTo(1f, tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing))
                        confirm()
                    }
                    waitForUpOrCancellation()
                    if (progress.value < 1f) {
                        fill.cancel()
                        scope.launch { progress.snapTo(0f) }
                    }
                }
            }
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)) {
            leading?.invoke()
            Text(text, color = tokens.colors.text, fontSize = tokens.textSizes.body)
        }
        val color = tokens.colors.text
        val faint = tokens.colors.divider
        Canvas(Modifier.fillMaxWidth().height(tokens.spacing.sectionGap)) {
            val dot = size.height / 2
            val count = (size.width / (dot * 3)).toInt().coerceAtLeast(1)
            repeat(count) { i ->
                val filled = (i + 1f) / count <= progress.value
                drawCircle(if (filled) color else faint, dot / 2, Offset(dot + i * size.width / count, size.height / 2))
            }
        }
    }
}
