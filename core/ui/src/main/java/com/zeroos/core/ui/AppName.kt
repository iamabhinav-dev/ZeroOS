package com.zeroos.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.zeroos.core.model.AppEntry
import com.zeroos.core.resources.R

/**
 * The name to show for an app: "II-WhatsApp" when several apps share the name (D-034), otherwise its label
 * (or custom name). Use this wherever an app's name is displayed so it's the same everywhere.
 */
@Composable
fun AppEntry.displayName(): String {
    val number = copyNumber ?: return label
    val numeral = stringArrayResource(R.array.app_copy_numerals).getOrNull(number - 1) ?: number.toString()
    return stringResource(R.string.app_copy_name, numeral, label)
}
