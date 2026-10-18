package com.zeroos.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.resources.R
import com.zeroos.core.ui.DotLine
import com.zeroos.core.ui.FocusMark
import com.zeroos.core.ui.ZeroTheme

/**
 * Home slot ⑤ (wireframe A4, D-051): the small Focus icon opens the picker (F9.1). While focusing it shows the icon,
 * "23m left" and a dot line that fills as the time passes; tapping it opens "End focus early?" (F9.4, F9.5, F9.9c).
 */
@Composable
fun FocusSlot(minutesLeft: Int?, progress: Float, icon: FocusIcon, onStart: () -> Unit, onEndEarly: () -> Unit) {
    val tokens = ZeroTheme.tokens
    if (minutesLeft == null) {
        Box(
            Modifier.sizeIn(minWidth = tokens.spacing.minTouchTarget, minHeight = tokens.spacing.minTouchTarget)
                .padding(horizontal = tokens.spacing.screenGutter)
                .clickable(onClick = onStart)
                .testTag(HomeTestTags.FOCUS),
            contentAlignment = Alignment.CenterStart
        ) {
            FocusMark(icon, tokens.spacing.focusMarkSmall, stringResource(R.string.focus_start))
        }
    } else {
        Column(
            Modifier.fillMaxWidth()
                .heightIn(min = tokens.spacing.minTouchTarget)
                .clickable(onClick = onEndEarly)
                .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
                .testTag(HomeTestTags.FOCUS)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)) {
                FocusMark(icon, tokens.spacing.focusMarkSmall, stringResource(R.string.focus_icon_description))
                Text(stringResource(R.string.focus_left, minutesLeft), color = tokens.colors.text, fontSize = tokens.textSizes.body)
            }
            DotLine(progress)
        }
    }
}
