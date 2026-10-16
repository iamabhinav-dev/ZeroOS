package com.zeroos.core.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The big clock (F1.1, F6.7): always one line, shrinking to fit when "hh:mm:ss" or a large text size doesn't fit
 * the width. Without this the last digits wrap and Doto's tight line spacing draws them over the first ones.
 */
@Composable
fun ClockText(text: String, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    Text(
        text = text,
        color = tokens.colors.text,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = tokens.textSizes.date, maxFontSize = tokens.textSizes.clock),
        modifier = modifier
    )
}
