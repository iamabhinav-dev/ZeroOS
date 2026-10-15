package com.zeroos.core.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Doto (SIL Open Font License 1.1; licence in assets/licenses), the dot-matrix font used for all text
 * (F6.3, D-020). The upstream file is a variable font; these are static instances made with fontTools
 * (`varLib.instancer wght=400|700 ROND=100`), so every device renders the same weight and fully round dots.
 * Bold is the default because Doto's dots grow with weight and thin dots are hard to read.
 */
val DotoFamily = FontFamily(
    Font(R.font.doto_regular, FontWeight.Normal),
    Font(R.font.doto_bold, FontWeight.Bold)
)
