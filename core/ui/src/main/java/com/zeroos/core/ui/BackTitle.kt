package com.zeroos.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.zeroos.core.resources.R

/**
 * The dotted ‹ back arrow (wireframes "← Title"): a 48 dp target read as "Back" (NFR-A1, NFR-A2). It calls the same
 * [onBack] as the system Back gesture, so it always goes to the page this one was opened from (D-052).
 */
@Composable
fun BackArrow(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    Box(
        modifier.size(tokens.spacing.minTouchTarget).clickable(role = Role.Button, onClick = onBack).testTag(BACK_ARROW_TAG),
        contentAlignment = Alignment.Center
    ) {
        DotMatrixIcon(
            DotIcons.Back,
            height = tokens.spacing.minTouchTarget * BACK_ARROW_SHARE,
            contentDescription = stringResource(R.string.common_back)
        )
    }
}

/** A page title with the back arrow before it; [trailing] holds extras such as a count (D-052). */
@Composable
fun BackTitle(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
    val tokens = ZeroTheme.tokens
    Row(
        modifier.fillMaxWidth().padding(start = tokens.spacing.listItemCompact, top = tokens.spacing.listItemCompact),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackArrow(onBack)
        Text(title, color = tokens.colors.text, fontSize = tokens.textSizes.title, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

/** How much of the 48 dp target the arrow fills (the size chosen for the wallpaper preview). */
private const val BACK_ARROW_SHARE = 0.75f

const val BACK_ARROW_TAG = "back_arrow"
