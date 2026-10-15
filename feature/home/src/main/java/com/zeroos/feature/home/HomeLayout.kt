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
 * both fit, top-block slots are dropped lowest priority first until they do.
 */
@Composable
fun HomeLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val favorites = measurables.firstOrNull { it.layoutId == FAVORITES }?.measure(loose)
        val slots = measurables.filter { it.layoutId is HomeSlot }.map { (it.layoutId as HomeSlot) to it.measure(loose) }

        val available = constraints.maxHeight - (favorites?.height ?: 0)
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
            favorites?.placeRelative(0, height - favorites.height)
        }
    }
}
