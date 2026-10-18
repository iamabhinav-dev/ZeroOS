package com.zeroos.feature.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.zeroos.core.resources.R
import com.zeroos.core.ui.TextAction
import com.zeroos.core.ui.ZeroTheme
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * Take a break (wireframe H14, F8.16): a line from list D and one button with a line from list E ([pun] and
 * [buttonPun] index those lists). The screen stays until the button is tapped; then the
 * button counts down [waitSeconds] and [onDone] removes the screen. There is no other way out.
 */
@Composable
fun BreakScreen(pun: Int, buttonPun: Int, waitSeconds: Int, onDone: () -> Unit) {
    val tokens = ZeroTheme.tokens
    var left by remember { mutableStateOf<Int?>(null) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(left) {
        val now = left ?: return@LaunchedEffect
        if (now <= 0) {
            done()
        } else {
            delay(1.seconds)
            left = now - 1
        }
    }
    Box(Modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(StopTestTags.BREAK)) {
        StopLayout(
            art = { BreathingDots(BREATH_SECONDS, still = rememberReducedMotion(), repeat = true) },
            buttons = {
                val countdown = left
                if (countdown == null) {
                    TextAction(
                        punLine(R.array.wellbeing_puns_break_button, buttonPun),
                        { left = waitSeconds },
                        modifier = Modifier.testTag(StopTestTags.BREAK_BUTTON)
                    )
                } else {
                    Text(
                        countdown.toString(),
                        color = tokens.colors.text,
                        fontSize = tokens.textSizes.title,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(tokens.spacing.sectionGap).semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag(StopTestTags.BREAK_BUTTON)
                    )
                }
            },
            buttonsArrangement = Arrangement.Center
        ) {
            Line(punLine(R.array.wellbeing_puns_break, pun))
        }
    }
}

/** One slow breath while the break screen waits. */
private const val BREATH_SECONDS = 8
