package com.zeroos.core.logging

/** Log severity, lowest to highest (F17.1). */
enum class LogLevel(val letter: Char) {
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E')
}
