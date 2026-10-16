package com.zeroos.core.ui

import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatTest {
    private val time = ZonedDateTime.parse("2026-10-15T16:05:00+05:30[Asia/Kolkata]")

    @Test
    fun f1_1_uses24HourClockWhenDeviceDoes() {
        assertEquals("16:05", ClockFormat(Locale.ENGLISH, is24Hour = true, "EEEE, d MMMM").time(time))
    }

    @Test
    fun f1_1_uses12HourClockWhenDeviceDoes() {
        assertEquals("4:05", ClockFormat(Locale.ENGLISH, is24Hour = false, "EEEE, d MMMM").time(time))
    }

    @Test
    fun f1_2_formatsWeekdayDayAndMonth() {
        assertEquals("Thursday, 15 October", ClockFormat(Locale.ENGLISH, true, "EEEE, d MMMM").date(time))
    }
}
