package com.zeroos.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zeroos.feature.applist.AppListRoute
import com.zeroos.feature.home.HomeRoute
import kotlinx.coroutines.flow.Flow

/**
 * Switches between home and the app list (Step 1.3). Pressing Home while ZeroOS is in front returns to
 * the home screen (Step 1.1); the list's own Back handler also closes it.
 */
@Composable
fun LauncherRoot(homePresses: Flow<Unit>) {
    var showAppList by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(homePresses) {
        homePresses.collect { showAppList = false }
    }
    if (showAppList) {
        AppListRoute(onClose = { showAppList = false })
    } else {
        HomeRoute(onSwipeUp = { showAppList = true })
    }
}
