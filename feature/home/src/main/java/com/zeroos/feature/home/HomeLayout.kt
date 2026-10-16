package com.zeroos.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId

private const val FAVORITES = "favorites"

/** Marks a top-block child with its slot. */
fun Modifier.homeSlot(slot: HomeSlot): Modifier = layoutId(slot)

/** Marks the favorites block, which always stays fully visible at the bottom. */
fun Modifier.homeFavorites(): Modifier = layoutId(FAVORITES)

/**
 * Lays out the top block at the top and favorites at the bottom of a non-scrolling screen (F1.6). If they don't
 * both fit, top-block slots are dropped lowest priority first until they do. In landscape (wider than tall) the
 * two sit side by side: top block on the left half, favorites on the right half, still at the bottom (NFR-R4).
 */
@Composable
fun HomeLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val landscape = constraints.hasBoundedHeight && constraints.maxWidth > constraints.maxHeight
        val columnWidth = if (landscape) constraints.maxWidth / 2 else constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, maxWidth = columnWidth, minHeight = 0)
        val favorites = measurables.firstOrNull { it.layoutId == FAVORITES }?.measure(loose)
        val slots = measurables.filter { it.layoutId is HomeSlot }.map { (it.layoutId as HomeSlot) to it.measure(loose) }

        val available = constraints.maxHeight - if (landscape) 0 else favorites?.height ?: 0
        var used = slots.sumOf { it.second.height }
        val dropped = mutableSetOf<HomeSlot>()
        slots.sortedBy { it.first.priority }.forEach { (slot, placeable) ->
            if (used > available && slot.priority != KEEP) {
                dropped += slot
                used -= placeable.height
            }
        }

        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else used + (favorites?.height ?: 0)
        layout(constraints.maxWidth, height) {
            var y = 0
            slots.filterNot { it.first in dropped }.forEach { (_, placeable) ->
                placeable.placeRelative(0, y)
                y += placeable.height
            }
            favorites?.placeRelative(if (landscape) columnWidth else 0, (height - favorites.height).coerceAtLeast(0))
        }
    }
}
