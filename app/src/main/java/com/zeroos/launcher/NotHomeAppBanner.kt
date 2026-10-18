package com.zeroos.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme

/** "ZeroOS isn't your home app · Set as default" at the top of home (wireframe A6, F14.7). */
@Composable
fun NotHomeAppBanner(onSetDefault: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    Column(modifier.fillMaxWidth().background(tokens.colors.background).testTag(NOT_HOME_APP_TAG)) {
        BodyText(stringResource(R.string.home_not_default))
        Row {
            TextAction(stringResource(R.string.home_set_default), onClick = onSetDefault)
            TextAction(stringResource(R.string.common_dismiss), onClick = onDismiss, muted = true)
        }
    }
}

const val NOT_HOME_APP_TAG = "not_home_app"
