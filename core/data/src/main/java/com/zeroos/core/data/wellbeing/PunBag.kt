package com.zeroos.core.data.wellbeing

import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Shuffle-bag order for the stop-screen puns (F8.13): every line once before any repeat, and never the same line
 * twice in a row, even where one shuffle ends and the next begins. Saved, so the position survives restarts.
 */
@Serializable
data class PunBag(val order: List<Int> = emptyList(), val next: Int = 0) {

    /** The next line out of [size], and the bag to save. A list that changed size starts a new shuffle. */
    fun draw(size: Int, random: Random): Pair<Int, PunBag> {
        require(size > 0) { "no lines" }
        val bag = if (order.size == size && next in order.indices) this else PunBag(shuffle(size, lastShown(size), random))
        return bag.order[bag.next] to bag.copy(next = bag.next + 1)
    }

    private fun lastShown(size: Int): Int? = if (order.size == size) order.getOrNull(next - 1) else null

    private fun shuffle(size: Int, last: Int?, random: Random): List<Int> {
        val order = (0 until size).shuffled(random).toMutableList()
        if (size > 1 && order.first() == last) {
            val swap = random.nextInt(1, size)
            order[0] = order[swap].also { order[swap] = order[0] }
        }
        return order
    }
}
