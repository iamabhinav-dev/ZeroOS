package com.zeroos.feature.applist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.model.AppEntry
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ZeroTheme

/** App list with its ViewModel (Step 1.3). Closes after an app starts. */
@Composable
fun AppListRoute(onClose: () -> Unit, viewModel: AppListViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    AppListScreen(
        apps = apps,
        onAppClick = { if (viewModel.launch(it.key)) onClose() },
        onClose = onClose
    )
}

/** Alphabetical text list of apps (F2.2; wireframe B1 without search, which comes in Step 2.4). Back closes it. */
@Composable
fun AppListScreen(apps: List<AppEntry>, onAppClick: (AppEntry) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onClose)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .safeDrawingPadding()
            .testTag(AppListTestTags.LIST),
        contentPadding = PaddingValues(vertical = tokens.spacing.screenGutter)
    ) {
        items(apps, key = { it.key.asString() }) { app ->
            MinimalListItem(text = app.label, onClick = { onAppClick(app) }, large = true)
        }
    }
}

object AppListTestTags {
    const val LIST = "app_list"
}
