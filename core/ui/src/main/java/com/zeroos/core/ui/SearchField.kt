package com.zeroos.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.zeroos.core.resources.R

/** Search box in Doto with a muted "search…" hint (wireframes B1, E3 picker). Opens the keyboard when [autoOpenKeyboard]. */
@Composable
fun SearchField(
    query: String,
    autoOpenKeyboard: Boolean,
    onQueryChange: (String) -> Unit,
    onGo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ZeroTheme.tokens
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoOpenKeyboard) {
        if (autoOpenKeyboard) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    val style = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.text, fontSize = tokens.textSizes.appLabel)
    val label = stringResource(R.string.app_list_search_label)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(tokens.colors.text),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .semantics { contentDescription = label }
            .padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemVertical),
        decorationBox = { field ->
            Box {
                if (query.isEmpty()) {
                    Text(
                        stringResource(R.string.app_list_search_hint),
                        style = style.copy(color = tokens.colors.textMuted)
                    )
                }
                field()
            }
        }
    )
}
