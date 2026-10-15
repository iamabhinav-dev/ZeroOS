package com.zeroos.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.zeroos.core.model.settings.TextSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZeroThemeTest {
    /** WCAG 2.1 contrast ratio. */
    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    @Test
    fun nfrA3_textMeetsWcagAaInBothThemes() {
        listOf(ZeroColors.Dark, ZeroColors.Light).forEach { colors ->
            assertTrue(contrast(colors.text, colors.background) >= 4.5)
            assertTrue("muted text ${colors.textMuted}", contrast(colors.textMuted, colors.background) >= 4.5)
        }
    }

    @Test
    fun f6_1_darkThemeIsPureBlack() {
        assertEquals(Color.Black, zeroTokens(dark = true, TextSize.MEDIUM).colors.background)
        assertEquals(Color.White, zeroTokens(dark = false, TextSize.MEDIUM).colors.background)
    }

    @Test
    fun f6_4_textSizeScaleAndMinimum() {
        val medium = ZeroTextSizes()
        assertEquals(medium, medium.scaled(TextSize.MEDIUM))
        assertEquals(medium.body.value * 1.3f, medium.scaled(TextSize.EXTRA_LARGE).body.value, 0.01f)
        val small = medium.scaled(TextSize.SMALL)
        listOf(small.body, small.small, small.date, small.appLabel, small.title).forEach {
            assertTrue("$it is below the Doto minimum", it.value >= ZeroTextSizes.MIN_SIZE_SP)
        }
        assertTrue(TextSize.entries.zipWithNext().all { (a, b) -> a.factor < b.factor })
    }
}
