package com.zeroos.launcher.diagnostics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BackArrow
import com.zeroos.core.ui.BackTitle
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.ScreenTitle
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme

/** Wireframe C2. */
@Composable
fun CrashReportsScreen(reports: List<CrashReportItem>, onOpen: (CrashReportItem) -> Unit, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    LazyColumn(modifier = Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding()) {
        item { BackTitle(stringResource(R.string.crash_reports_title), onBack) }
        if (reports.isEmpty()) item { BodyText(stringResource(R.string.crash_reports_empty), muted = true) }
        items(reports, key = { it.fileName }) { report ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.spacing.minTouchTarget)
                    .clickable { onOpen(report) }
                    .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
            ) {
                Text(text = report.fileName.removeSuffix(".txt"), color = tokens.colors.text, fontSize = tokens.textSizes.body)
                Text(
                    text = report.title,
                    color = tokens.colors.textMuted,
                    fontSize = tokens.textSizes.small
                )
            }
        }
    }
}

/** One full crash report: stack trace and the last 50 log entries. */
@Composable
fun CrashReportScreen(text: String, onBack: () -> Unit) {
    val tokens = ZeroTheme.tokens
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        BackArrow(onBack)
        Text(
            text = text,
            color = tokens.colors.text,
            fontSize = tokens.textSizes.small,
            modifier = Modifier.padding(tokens.spacing.screenGutter)
        )
    }
}

/** Crash banner on home, shown once per new crash (F17.10, wireframe C3). */
@Composable
fun CrashBanner(onView: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    Column(modifier = modifier.fillMaxWidth().background(tokens.colors.background).testTag(DiagnosticsTestTags.CRASH_BANNER)) {
        BodyText(stringResource(R.string.crash_banner_message))
        Row {
            TextAction(stringResource(R.string.crash_banner_view), onClick = onView)
            TextAction(stringResource(R.string.common_dismiss), onClick = onDismiss)
        }
    }
}
