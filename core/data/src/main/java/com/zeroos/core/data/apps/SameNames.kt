package com.zeroos.core.data.apps

import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.ProfileType

/**
 * Numbers apps that share a name, e.g. WhatsApp and its App Clone (D-034). The main profile's app comes first,
 * then other profiles by serial number, so the numbers stay the same across restarts. List order is kept.
 */
fun List<AppEntry>.numberSameNames(): List<AppEntry> {
    val numbers = groupBy { it.label.trim().lowercase() }
        .values
        .filter { it.size > 1 }
        .flatMap { group ->
            group.sortedWith(compareBy<AppEntry>({ it.profile != ProfileType.MAIN }, { it.key.userSerial }, { it.key.asString() }))
                .mapIndexed { index, app -> app.key to index + 1 }
        }
        .toMap()
    return map { app -> numbers[app.key]?.let { app.copy(copyNumber = it) } ?: app }
}

/** Shows [name] instead of the app's label; a custom name is shown exactly as typed, without a number (D-034). */
fun AppEntry.renamedTo(name: String?): AppEntry = if (name == null) this else copy(label = name, copyNumber = null)
