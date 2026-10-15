package com.zeroos.feature.home

/**
 * Decides whether a finished vertical drag is a swipe up (F4.1). Values are in pixels and pixels/second;
 * negative means upward. A drag counts if it travels [minDistance], or if it's a fast fling of at least
 * half that distance.
 */
fun isSwipeUp(dragY: Float, velocityY: Float, minDistance: Float, minVelocity: Float): Boolean =
    dragY <= -minDistance || (velocityY <= -minVelocity && dragY <= -minDistance / 2)
