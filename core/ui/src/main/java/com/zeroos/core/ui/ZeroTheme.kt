package com.zeroos.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal design tokens for Phase 1 (pure black, monochrome). Step 2.2 adds light/system themes,
 * the Doto clock font, text-size scale and the rest of the design system (F6, NFR-L8).
 */
private val PureBlack = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Gray = Color(0xFF9E9E9E)

@Immutable
data class ZeroColors(
    val background: Color = PureBlack,
    val text: Color = White,
    val textMuted: Color = Gray
)

@Immutable
data class ZeroSpacing(
    val screenGutter: Dp = 24.dp,
    val listItemVertical: Dp = 12.dp,
    val listItemCompact: Dp = 4.dp,
    val sectionGap: Dp = 8.dp,
    val minTouchTarget: Dp = 48.dp
)

@Immutable
data class ZeroTextSizes(
    val clock: TextUnit = 72.sp,
    val date: TextUnit = 18.sp,
    val appLabel: TextUnit = 22.sp,
    val title: TextUnit = 28.sp,
    val body: TextUnit = 16.sp,
    val mono: TextUnit = 12.sp
)

/** Gesture thresholds from REQUIREMENTS.md Appendix A. */
@Immutable
data class ZeroGestures(
    val swipeDistance: Dp = 48.dp,
    /** Minimum fling speed, in dp per second. */
    val swipeVelocity: Dp = 300.dp
)

@Immutable
data class ZeroTokens(
    val colors: ZeroColors = ZeroColors(),
    val spacing: ZeroSpacing = ZeroSpacing(),
    val textSizes: ZeroTextSizes = ZeroTextSizes(),
    val gestures: ZeroGestures = ZeroGestures()
)

private val LocalZeroTokens = staticCompositionLocalOf { ZeroTokens() }

/** Access tokens from composables: `ZeroTheme.tokens.spacing.screenGutter`. */
object ZeroTheme {
    val tokens: ZeroTokens
        @Composable get() = LocalZeroTokens.current
}

@Composable
fun ZeroTheme(tokens: ZeroTokens = ZeroTokens(), content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        background = tokens.colors.background,
        surface = tokens.colors.background,
        onBackground = tokens.colors.text,
        onSurface = tokens.colors.text
    )
    CompositionLocalProvider(LocalZeroTokens provides tokens) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
