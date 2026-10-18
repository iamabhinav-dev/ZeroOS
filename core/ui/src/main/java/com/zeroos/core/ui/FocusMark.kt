package com.zeroos.core.ui

import android.content.res.Resources
import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.zeroos.core.model.settings.FocusIcon
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Focus icon (F9.8): a dotted circle with the F as the empty gap, in the theme's text colour. The dots come
 * from res/raw (copied from docs/brand/focus-marks/), read once off the main thread (NFR-P1).
 */
@Composable
fun FocusMark(icon: FocusIcon, size: Dp, contentDescription: String, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    val dots by produceState<FloatArray?>(FocusMarkDots.cached(icon), icon) {
        value = withContext(Dispatchers.IO) { FocusMarkDots.load(resources, icon) }
    }
    val color = ZeroTheme.tokens.colors.text
    Canvas(modifier.size(size).semantics { this.contentDescription = contentDescription }) {
        val d = dots ?: return@Canvas
        val scale = this.size.minDimension / FocusMarkDots.VIEW_BOX
        for (i in d.indices step 3) drawCircle(color, d[i + 2] * scale, Offset(d[i] * scale, d[i + 1] * scale))
    }
}

/** Reads every Focus icon's dots now (call off the main thread); used by tests so screenshots don't race the load. */
fun preloadFocusMarks(resources: Resources) = FocusIcon.entries.forEach { FocusMarkDots.load(resources, it) }

/** Dot lists (x, y, radius in a 200-unit box) per design, cached after the first read. */
internal object FocusMarkDots {
    const val VIEW_BOX = 200f
    private val cache = ConcurrentHashMap<FocusIcon, FloatArray>()

    fun cached(icon: FocusIcon): FloatArray? = cache[icon]

    fun load(resources: Resources, icon: FocusIcon): FloatArray = cache.getOrPut(icon) {
        resources.openRawResource(rawRes(icon)).bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }.flatMap { line -> line.split(' ').map(String::toFloat) }.toList().toFloatArray()
        }
    }

    @RawRes
    private fun rawRes(icon: FocusIcon) = when (icon) {
        FocusIcon.MIX -> R.raw.focus_mix
        FocusIcon.SPHERE -> R.raw.focus_sphere
        FocusIcon.RIM -> R.raw.focus_rim
        FocusIcon.FLOW -> R.raw.focus_flow
    }
}
