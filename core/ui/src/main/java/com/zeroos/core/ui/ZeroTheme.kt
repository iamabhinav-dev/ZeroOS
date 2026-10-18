package com.zeroos.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeroos.core.model.settings.TextSize
import com.zeroos.core.model.settings.ThemeMode

/**
 * ZeroOS design system (Step 2.2): monochrome dark/light palettes, Doto for all text (F6.3, D-020), a text-size
 * scale (F6.4), spacing and gesture tokens (NFR-L8). Screens read tokens via `ZeroTheme.tokens`.
 */
private val PureBlack = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val GrayOnBlack = Color(0xFF9E9E9E)
private val GrayOnWhite = Color(0xFF5F5F5F)
private val DividerOnBlack = Color(0xFF333333)
private val DividerOnWhite = Color(0xFFDDDDDD)
private val FaintOnBlack = Color(0xFF555555)
private val FaintOnWhite = Color(0xFFB5B5B5)

/** Dims the phone wallpaper behind home so text stays readable (F6.2). */
private val WallpaperScrim = Color(0x99000000)

@Immutable
data class ZeroColors(
    val background: Color,
    val text: Color,
    val textMuted: Color,
    val divider: Color,
    /**
     * Pushed into the background while something else is in focus, e.g. the other letters while sliding along the
     * alphabet scroller (F2.7). Below WCAG AA on purpose, so never for text that must be read at rest.
     */
    val textFaint: Color,
    /** Drawn over the phone wallpaper on home (F6.2). */
    val wallpaperScrim: Color = WallpaperScrim
) {
    companion object {
        /** Pure black for OLED (F6.1 default). */
        val Dark =
            ZeroColors(background = PureBlack, text = White, textMuted = GrayOnBlack, divider = DividerOnBlack, textFaint = FaintOnBlack)
        val Light =
            ZeroColors(background = White, text = PureBlack, textMuted = GrayOnWhite, divider = DividerOnWhite, textFaint = FaintOnWhite)
    }
}

@Immutable
data class ZeroSpacing(
    val screenGutter: Dp = 24.dp,
    val listItemVertical: Dp = 12.dp,
    val listItemCompact: Dp = 4.dp,
    val sectionGap: Dp = 8.dp,
    val minTouchTarget: Dp = 48.dp,
    /** Thin borders, e.g. around the appearance preview. */
    val hairline: Dp = 1.dp,
    /** Outline of the selected wallpaper in the gallery (F6.8). */
    val selectedOutline: Dp = 3.dp,
    /** Width of a wallpaper thumbnail; its height follows the phone's shape (F6.8). */
    val thumbnailWidth: Dp = 96.dp,
    /** Height of the "ZeroOS" wordmark (D-037). */
    val wordmarkHeight: Dp = 28.dp,
    /** Width of the dot art on the wellbeing stop screens (F8.15). */
    val stopArtWidth: Dp = 160.dp
)

/** Text sizes at "Medium"; [scaled] applies the S/M/L/XL setting (F6.4). The system font scale applies on top via sp. */
@Immutable
data class ZeroTextSizes(
    val clock: TextUnit = 72.sp,
    val date: TextUnit = 18.sp,
    val appLabel: TextUnit = 22.sp,
    val title: TextUnit = 28.sp,
    val body: TextUnit = 17.sp,
    /** Smallest text (log lines, captions). Never below [MIN_SIZE_SP] after scaling. */
    val small: TextUnit = 15.sp
) {
    fun scaled(textSize: TextSize): ZeroTextSizes {
        val factor = textSize.factor
        fun TextUnit.scale(min: Float = 0f) = (value * factor).coerceAtLeast(min).sp
        return ZeroTextSizes(
            clock = clock.scale(),
            date = date.scale(MIN_SIZE_SP),
            appLabel = appLabel.scale(MIN_SIZE_SP),
            title = title.scale(MIN_SIZE_SP),
            body = body.scale(MIN_SIZE_SP),
            small = small.scale(MIN_SIZE_SP)
        )
    }

    companion object {
        /** Doto stays readable from about 14 sp (F6.4). */
        const val MIN_SIZE_SP = 14f
    }
}

/** Multipliers for the text-size setting (F6.4). */
val TextSize.factor: Float
    get() = when (this) {
        TextSize.SMALL -> 0.85f
        TextSize.MEDIUM -> 1f
        TextSize.LARGE -> 1.15f
        TextSize.EXTRA_LARGE -> 1.3f
    }

/** Gesture thresholds from REQUIREMENTS.md Appendix A. */
@Immutable
data class ZeroGestures(
    val swipeDistance: Dp = 48.dp,
    /** Minimum fling speed, in dp per second. */
    val swipeVelocity: Dp = 300.dp
)

@Immutable
data class ZeroTokens(
    val colors: ZeroColors = ZeroColors.Dark,
    val spacing: ZeroSpacing = ZeroSpacing(),
    val textSizes: ZeroTextSizes = ZeroTextSizes(),
    val gestures: ZeroGestures = ZeroGestures(),
    val isDark: Boolean = true
)

private val LocalZeroTokens = staticCompositionLocalOf { ZeroTokens() }

/** Access tokens from composables: `ZeroTheme.tokens.spacing.screenGutter`. */
object ZeroTheme {
    val tokens: ZeroTokens
        @Composable get() = LocalZeroTokens.current
}

/** Builds tokens for the appearance settings (F6.1, F6.4). */
fun zeroTokens(dark: Boolean, textSize: TextSize): ZeroTokens = ZeroTokens(
    colors = if (dark) ZeroColors.Dark else ZeroColors.Light,
    textSizes = ZeroTextSizes().scaled(textSize),
    isDark = dark
)

/** Applies the ZeroOS theme. Every `Text` inside uses Doto by default (D-020). */
@Composable
fun ZeroTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    textSize: TextSize = TextSize.MEDIUM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val tokens = zeroTokens(dark, textSize)
    val colors = tokens.colors
    val scheme = if (dark) {
        darkColorScheme(
            background = colors.background,
            surface = colors.background,
            onBackground = colors.text,
            onSurface = colors.text,
            primary = colors.text,
            onPrimary = colors.background
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.background,
            onBackground = colors.text,
            onSurface = colors.text,
            primary = colors.text,
            onPrimary = colors.background
        )
    }
    CompositionLocalProvider(LocalZeroTokens provides tokens) {
        MaterialTheme(colorScheme = scheme, typography = dotoTypography(tokens.textSizes), content = content)
    }
}

/** Material typography with Doto in every style, so dialogs and fields use it too. */
internal fun dotoTypography(sizes: ZeroTextSizes): Typography {
    // Bold by default: Doto dots grow with weight, and thin dots are hard to read (F6.4).
    val base = TextStyle(fontFamily = DotoFamily, fontWeight = FontWeight.Bold)
    val defaults = Typography()
    return Typography(
        displayLarge = defaults.displayLarge.merge(base),
        displayMedium = defaults.displayMedium.merge(base),
        displaySmall = defaults.displaySmall.merge(base),
        headlineLarge = defaults.headlineLarge.merge(base),
        headlineMedium = defaults.headlineMedium.merge(base),
        headlineSmall = defaults.headlineSmall.merge(base).copy(fontSize = sizes.title),
        titleLarge = defaults.titleLarge.merge(base).copy(fontSize = sizes.title),
        titleMedium = defaults.titleMedium.merge(base).copy(fontSize = sizes.body),
        titleSmall = defaults.titleSmall.merge(base).copy(fontSize = sizes.small),
        bodyLarge = defaults.bodyLarge.merge(base).copy(fontSize = sizes.body),
        bodyMedium = defaults.bodyMedium.merge(base).copy(fontSize = sizes.body),
        bodySmall = defaults.bodySmall.merge(base).copy(fontSize = sizes.small),
        labelLarge = defaults.labelLarge.merge(base).copy(fontSize = sizes.body),
        labelMedium = defaults.labelMedium.merge(base).copy(fontSize = sizes.small),
        labelSmall = defaults.labelSmall.merge(base).copy(fontSize = sizes.small)
    )
}
