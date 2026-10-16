package com.zeroos.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeTest {
    private val distance = 100f
    private val velocity = 600f
    private fun swipe(dx: Float, dy: Float, vx: Float = 0f, vy: Float = 0f) = swipeOf(Offset(dx, dy), Velocity(vx, vy), distance, velocity)

    @Test
    fun f4_1_longUpwardDragIsSwipeUp() = assertEquals(Swipe.UP, swipe(0f, -120f))

    @Test
    fun f4_1_fastShortFlickIsSwipeUp() = assertEquals(Swipe.UP, swipe(0f, -60f, vy = -900f))

    @Test
    fun f4_1_tinyFastFlickIsNothing() = assertNull(swipe(0f, -20f, vy = -2000f))

    @Test
    fun f4_1_slowShortDragIsNothing() = assertNull(swipe(0f, -60f, vy = -100f))

    @Test
    fun f4_4_downwardDragIsSwipeDown() = assertEquals(Swipe.DOWN, swipe(10f, 150f))

    @Test
    fun f4_2_f4_3_horizontalSwipes() {
        assertEquals(Swipe.LEFT, swipe(-150f, 20f))
        assertEquals(Swipe.RIGHT, swipe(150f, -30f))
    }

    @Test
    fun f4_mostlyDiagonalPicksLongerAxis() = assertEquals(Swipe.UP, swipe(-90f, -130f))

    @Test
    fun f4_flingAgainstDragDirectionIsNothing() = assertNull(swipe(0f, -60f, vy = 900f))
}
