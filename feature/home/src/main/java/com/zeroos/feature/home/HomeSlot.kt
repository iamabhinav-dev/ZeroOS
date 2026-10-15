package com.zeroos.feature.home

/**
 * Lines in the top block of home, in screen order (wireframe A1). [priority] decides what's dropped first when
 * space runs out: to-do, then calendar, then next alarm (A1 layout rules). Favorites are never dropped (F1.6).
 */
enum class HomeSlot(val priority: Int) {
    CLOCK(KEEP),
    DATE(KEEP),
    NEXT_ALARM(DROPPED_THIRD),
    STATS(KEEP),
    FOCUS(KEEP),
    CALENDAR(DROPPED_SECOND),
    TODO(DROPPED_FIRST)
}

/** Priority of slots that are never dropped. */
const val KEEP = Int.MAX_VALUE

private const val DROPPED_FIRST = 1
private const val DROPPED_SECOND = 2
private const val DROPPED_THIRD = 3
