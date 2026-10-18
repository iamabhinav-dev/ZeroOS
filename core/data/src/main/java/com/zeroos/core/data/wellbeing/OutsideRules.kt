package com.zeroos.core.data.wellbeing

import com.zeroos.core.model.settings.WellbeingSettings

/** What the Accessibility service shows over another app (wireframes H7b, H14). */
sealed interface Overlay {
    val packageName: String

    /** Today's limit used up, outside ZeroOS (F8.7). */
    data class Limit(override val packageName: String, val usedMinutes: Int, val extrasLeft: Int) : Overlay

    /** In the app too long without a break (F8.16). */
    data class Break(override val packageName: String, val waitSeconds: Int) : Overlay
}

/** Result of checking the app in front: an overlay to show now, and when to check again (one timer, NFR-P1). */
data class OutsideCheck(val overlay: Overlay? = null, val checkAgainInMillis: Long? = null)

/** F8.17: leaving for less than this doesn't end a session. */
const val BREAK_RESET_GAP_MILLIS = 60_000L

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * When the current unbroken session of [packageName] began, or null if it isn't in front at [nowMillis]. Gaps
 * shorter than [BREAK_RESET_GAP_MILLIS] (switching activities, a quick reply elsewhere) don't break it (F8.17).
 * [knownInFront]: the caller just saw the app come to the front, even if Android hasn't recorded it yet.
 */
fun currentSessionStart(
    events: List<UsageEvent>,
    packageName: String,
    windowStartMillis: Long,
    nowMillis: Long,
    knownInFront: Boolean = false
): Long? {
    val intervals = mutableListOf<LongArray>()
    var openSince: Long? = null
    for (event in events.filter { it.packageName == packageName }.sortedBy { it.timeMillis }) {
        when (event.type) {
            UsageEvent.Type.RESUMED -> if (openSince == null) openSince = event.timeMillis

            UsageEvent.Type.PAUSED -> {
                intervals += longArrayOf(openSince ?: windowStartMillis, event.timeMillis)
                openSince = null
            }

            UsageEvent.Type.UNLOCKED -> Unit
        }
    }
    val open = openSince ?: nowMillis.takeIf { knownInFront } ?: return null
    var start = open
    for (interval in intervals.asReversed()) {
        if (start - interval[1] >= BREAK_RESET_GAP_MILLIS) break
        start = minOf(start, interval[0])
    }
    return start
}

/** Everything the outside check needs, read once per check. */
data class OutsideFacts(
    val events: List<UsageEvent>,
    val windowStartMillis: Long,
    val dayStartMillis: Long,
    val nowMillis: Long,
    val extrasUsed: Int,
    /** When the last break over this app ended; the session counts from then (F8.16). */
    val lastBreakMillis: Long? = null,
    val knownInFront: Boolean = false
)

/**
 * Checks [packageName] outside ZeroOS: a used-up limit (F8.7) wins over a due break (F8.16). Otherwise says when
 * the nearer of the two will be due, so the service sets a single timer instead of polling.
 */
fun WellbeingSettings.outsideCheck(packageName: String, facts: OutsideFacts): OutsideCheck {
    val rule = apps[packageName]?.takeIf { enabled }
    val session = rule?.let {
        currentSessionStart(facts.events, packageName, facts.windowStartMillis, facts.nowMillis, facts.knownInFront)
    }
    if (rule == null || session == null) return OutsideCheck()
    val limit = limitCheck(packageName, facts)
    val pause = if (breaksEnabled && rule.takeBreak) breakCheck(packageName, session, facts) else OutsideCheck()
    return when {
        limit.overlay != null -> limit
        pause.overlay != null -> pause.copy(checkAgainInMillis = limit.checkAgainInMillis)
        else -> OutsideCheck(checkAgainInMillis = listOfNotNull(limit.checkAgainInMillis, pause.checkAgainInMillis).minOrNull())
    }
}

/** F8.7: the limit block, or how long until today's limit is used up; nothing when outside is off. */
private fun WellbeingSettings.limitCheck(packageName: String, facts: OutsideFacts): OutsideCheck {
    val limit = limitMinutesToday(packageName, facts.extrasUsed)?.takeIf { outsideEnforcement }?.times(MILLIS_PER_MINUTE)
    val today = facts.events.filter { it.timeMillis >= facts.dayStartMillis }
    val used = if (limit == null) 0 else foregroundMillis(today, facts.dayStartMillis, facts.nowMillis)[packageName] ?: 0
    return when {
        limit == null -> OutsideCheck()

        used >= limit -> OutsideCheck(
            Overlay.Limit(packageName, (used / MILLIS_PER_MINUTE).toInt(), (extraMinutesPerDay - facts.extrasUsed).coerceAtLeast(0))
        )

        else -> OutsideCheck(checkAgainInMillis = limit - used)
    }
}

/** F8.16: the break screen, or how long until it's due, counted from the session start or the last break. */
private fun WellbeingSettings.breakCheck(packageName: String, session: Long, facts: OutsideFacts): OutsideCheck {
    val elapsed = facts.nowMillis - maxOf(session, facts.lastBreakMillis ?: session)
    val after = breakAfterMinutes * MILLIS_PER_MINUTE
    return if (elapsed >=
        after
    ) {
        OutsideCheck(Overlay.Break(packageName, breakWaitSeconds))
    } else {
        OutsideCheck(checkAgainInMillis = after - elapsed)
    }
}
