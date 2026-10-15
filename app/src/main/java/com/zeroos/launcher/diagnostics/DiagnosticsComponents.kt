package com.zeroos.launcher.diagnostics

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.StoredLogEntry
import com.zeroos.core.resources.R
import com.zeroos.core.ui.ZeroTheme
import java.io.File
import kotlinx.coroutines.launch

/** Small building blocks shared by the Diagnostics screens (wireframe C). */

@Composable
internal fun Title(text: String) {
    val tokens = ZeroTheme.tokens
    Text(
        text = text,
        color = tokens.colors.text,
        fontSize = tokens.textSizes.title,
        modifier = Modifier.padding(tokens.spacing.screenGutter)
    )
}

@Composable
internal fun Body(text: String, muted: Boolean = false) {
    val tokens = ZeroTheme.tokens
    Text(
        text = text,
        color = if (muted) tokens.colors.textMuted else tokens.colors.text,
        fontSize = tokens.textSizes.body,
        modifier = Modifier.padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
    )
}

@Composable
internal fun Action(text: String, muted: Boolean = false, compact: Boolean = false, onClick: () -> Unit) {
    val tokens = ZeroTheme.tokens
    Box(
        modifier = Modifier
            .heightIn(min = tokens.spacing.minTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) tokens.spacing.sectionGap else tokens.spacing.screenGutter),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, color = if (muted) tokens.colors.textMuted else tokens.colors.text, fontSize = tokens.textSizes.body)
    }
}

@Composable
internal fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    val tokens = ZeroTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.minTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.screenGutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = tokens.colors.text, fontSize = tokens.textSizes.body, modifier = Modifier.weight(1f))
        Text(text = value, color = tokens.colors.textMuted, fontSize = tokens.textSizes.body)
    }
}
