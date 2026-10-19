package com.zeroos.feature.settings

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeroos.core.data.backup.BackupAge
import com.zeroos.core.data.backup.SettingsBackup
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BackTitle
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.ClockFormat
import com.zeroos.core.ui.MinimalListItem
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun BackupRoute(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
        uri?.let { viewModel.export { resolver.openOutputStream(it) } }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.read { resolver.openInputStream(it) } }
    }
    BackupScreen(
        state = state,
        onExport = { createFile.launch(viewModel.suggestedFileName()) },
        onImport = { openFile.launch(arrayOf(JSON, ANY_TEXT)) },
        onConfirmImport = viewModel::confirmImport,
        onCancelImport = viewModel::cancelImport,
        onBack = onBack
    )
}

private const val JSON = "application/json"
private const val ANY_TEXT = "text/*"

/** Skeleton for "16 Oct". */
private const val DAY_MONTH = "dMMM"

/** Wireframe F6. */
@Composable
fun BackupScreen(
    state: BackupState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onBack: () -> Unit
) {
    val tokens = ZeroTheme.tokens
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(BackupTestTags.SCREEN)) {
        BackTitle(stringResource(R.string.backup_title), onBack)
        MinimalListItem(stringResource(R.string.backup_export), onClick = onExport, trailing = CHEVRON)
        MinimalListItem(stringResource(R.string.backup_import), onClick = onImport, trailing = CHEVRON)
        val locale = LocalConfiguration.current.locales[0]
        val clock = remember(locale) { ClockFormat.forDevice(context, locale) }
        val last = state.lastExportMillis?.let {
            val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            val day = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, DAY_MONTH), locale).format(time)
            stringResource(R.string.backup_last_export, stringResource(R.string.common_date_time, day, clock.time(time)))
        } ?: stringResource(R.string.backup_never_exported)
        BodyText(last, muted = true, small = true)
        state.message?.let { BodyText(it.text()) }
    }
    if (state.pending != null) {
        AlertDialog(
            onDismissRequest = onCancelImport,
            containerColor = tokens.colors.background,
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = { ImportWarning(state.pending, state.pendingAge) },
            confirmButton = { TextAction(stringResource(R.string.backup_confirm_import), onClick = onConfirmImport, compact = true) },
            dismissButton = { TextAction(stringResource(R.string.common_cancel), onClick = onCancelImport, compact = true) }
        )
    }
}

/** Wireframe K1 (F13.6): what an import replaces, the backup's version if it differs, and what isn't in a backup. */
@Composable
private fun ImportWarning(backup: SettingsBackup, age: BackupAge) {
    val tokens = ZeroTheme.tokens
    val version = backup.appVersionName.orEmpty()
    val lines = listOfNotNull(
        stringResource(R.string.backup_confirm_body) to false,
        when (age) {
            BackupAge.OLDER -> stringResource(R.string.backup_confirm_older, version) to false
            BackupAge.NEWER -> stringResource(R.string.backup_confirm_newer, version) to false
            BackupAge.SAME, BackupAge.UNKNOWN -> null
        },
        stringResource(R.string.backup_confirm_permissions) to true
    )
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)) {
        lines.forEach { (line, muted) ->
            Text(line, color = if (muted) tokens.colors.textMuted else tokens.colors.text, fontSize = tokens.textSizes.body)
        }
    }
}

@Composable
private fun BackupMessage.text(): String = when (this) {
    BackupMessage.Exported -> stringResource(R.string.backup_exported)
    BackupMessage.Imported -> stringResource(R.string.backup_imported)
    BackupMessage.NotABackup -> stringResource(R.string.backup_not_a_backup)
    is BackupMessage.TooNew -> stringResource(R.string.backup_too_new, version)
    BackupMessage.Failed -> stringResource(R.string.backup_failed)
}

/** Doto has "›" (wireframe F). */
internal const val CHEVRON = "›"

object BackupTestTags {
    const val SCREEN = "backup"
}
