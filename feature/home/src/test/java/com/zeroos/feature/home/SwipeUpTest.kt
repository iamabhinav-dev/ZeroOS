package com.zeroos.feature.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeUpTest {
    private val distance = 100f
    private val velocity = 600f

    @Test
    fun f4_1_longUpwardDragIsSwipeUp() = assertTrue(isSwipeUp(-120f, 0f, distance, velocity))

    @Test
    fun f4_1_fastShortFlickIsSwipeUp() = assertTrue(isSwipeUp(-60f, -900f, distance, velocity))

    @Test
    fun f4_1_tinyFastFlickIsNotSwipeUp() = assertFalse(isSwipeUp(-20f, -2000f, distance, velocity))

    @Test
    fun f4_1_slowShortDragIsNotSwipeUp() = assertFalse(isSwipeUp(-60f, -100f, distance, velocity))

    @Test
    fun f4_1_downwardDragIsNotSwipeUp() = assertFalse(isSwipeUp(200f, 900f, distance, velocity))
}
