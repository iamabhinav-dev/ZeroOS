package com.zeroos.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.zeroos.core.model.settings.FocusIcon
import com.zeroos.core.resources.R
import com.zeroos.core.ui.FocusMark
import com.zeroos.core.ui.HoldToConfirm
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import kotlin.time.Duration.Companion.seconds

/**
 * Home slot ⑤ (wireframe A4): "focus" opens the picker (F9.1); while a session runs it shows the small Focus icon
 * and "focus · 23m left", and holding it for 3 s ends focus early (F9.4, F9.5, F9.9c).
 */
@Composable
fun FocusSlot(minutesLeft: Int?, icon: FocusIcon, onStart: () -> Unit, onEnd: () -> Unit) {
    if (minutesLeft == null) {
        TextAction(stringResource(R.string.focus_button), onStart, modifier = Modifier.testTag(HomeTestTags.FOCUS))
    } else {
        HoldToConfirm(
            text = stringResource(R.string.focus_left, minutesLeft),
            duration = END_FOCUS_HOLD,
            onConfirm = onEnd,
            modifier = Modifier.testTag(HomeTestTags.FOCUS),
            leading = { FocusMark(icon, ZeroTheme.tokens.spacing.focusMarkSmall, stringResource(R.string.focus_icon_description)) }
        )
    }
}

/** F9.5, Appendix A: hold 3 s. */
private val END_FOCUS_HOLD = 3.seconds
