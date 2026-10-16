package com.zeroos.core.ui

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats the home clock and date (F1.1, F1.2) following the device's 12/24-hour setting and locale (NFR-L2).
 * [datePattern] is the locale's best pattern for weekday + day + month, e.g. "EEEE, d MMMM" in English.
 */
class ClockFormat(locale: Locale, is24Hour: Boolean, datePattern: String) {
    private val time = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm", locale)
    private val date = DateTimeFormatter.ofPattern(datePattern, locale)

    fun time(value: ZonedDateTime): String = time.format(value)

    fun date(value: ZonedDateTime): String = date.format(value)

    companion object {
        /** The device's clock format, with the locale's weekday + day + month pattern (F1.1, F1.2, NFR-L2). */
        fun forDevice(context: android.content.Context, locale: Locale): ClockFormat = ClockFormat(
            locale,
            android.text.format.DateFormat.is24HourFormat(context),
            android.text.format.DateFormat.getBestDateTimePattern(locale, DATE_SKELETON)
        )

        /** Skeleton for weekday, day and month, passed to `DateFormat.getBestDateTimePattern`. */
        const val DATE_SKELETON = "EEEEdMMMM"
    }
}
