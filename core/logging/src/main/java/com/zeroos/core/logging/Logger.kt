package com.zeroos.core.logging

/**
 * The only logging API in ZeroOS (F17.1). Never pass private content (F17.8): notification text,
 * intention text, search queries, to-do text, custom app names, the custom block message, or other
 * apps' screen content. Use [redacted] for free-text values.
 */
interface Logger {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}

fun Logger.debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)

fun Logger.info(tag: String, message: String) = log(LogLevel.INFO, tag, message)

fun Logger.warn(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)

fun Logger.error(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

/** Replaces a free-text value with its length so it can be logged without its content (F17.8). */
fun redacted(value: String): String = "<redacted, ${value.length} chars>"
