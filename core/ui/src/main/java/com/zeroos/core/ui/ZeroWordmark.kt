package com.zeroos.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.zeroos.core.resources.R

/** The dotted "ZeroOS" wordmark from logo.svg (D-037), in the theme's text color. */
@Composable
fun ZeroWordmark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(com.zeroos.core.ui.R.drawable.zeroos_wordmark),
        contentDescription = stringResource(R.string.app_name),
        colorFilter = ColorFilter.tint(ZeroTheme.tokens.colors.text),
        modifier = modifier.height(ZeroTheme.tokens.spacing.wordmarkHeight)
    )
}
