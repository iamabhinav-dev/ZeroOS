package com.zeroos.core.data.apps

import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.settings.LauncherSettings

/** Visible apps: hidden ones removed (F5.1), custom names applied (F5.4). */
fun visibleApps(apps: List<AppEntry>, settings: LauncherSettings): List<AppEntry> = apps
    .filterNot { it.key.asString() in settings.hiddenApps }
    .map { app -> settings.renamedApps[app.key.asString()]?.let { app.copy(label = it) } ?: app }
