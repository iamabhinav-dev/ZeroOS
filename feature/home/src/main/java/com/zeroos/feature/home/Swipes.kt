package com.zeroos.feature.home

import kotlin.math.abs

/** Home-screen swipe directions (F4). */
enum class Swipe { UP, DOWN, LEFT, RIGHT }

/**
 * Decides which swipe a finished drag was, if any (F4.1–F4.4). Values are in pixels and pixels/second
 * (screen coordinates: up and left are negative). The longer axis wins. A drag counts if it travels
 * [minDistance], or if it's a fast fling of at least half that distance.
 */
fun swipeOf(dx: Float, dy: Float, vx: Float, vy: Float, minDistance: Float, minVelocity: Float): Swipe? {
    val vertical = abs(dy) >= abs(dx)
    val distance = if (vertical) dy else dx
    val speed = if (vertical) vy else vx
    val counts = abs(distance) >= minDistance || (abs(speed) >= minVelocity && abs(distance) >= minDistance / 2 && speed * distance > 0)
    return when {
        !counts -> null
        vertical -> if (distance < 0) Swipe.UP else Swipe.DOWN
        else -> if (distance < 0) Swipe.LEFT else Swipe.RIGHT
    }
}
