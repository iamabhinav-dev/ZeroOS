package com.zeroos.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight

/** Wireframe K3: a release document with the back arrow (D-052); scrolls. */
@Composable
fun DocumentReader(title: String, text: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = ZeroTheme.tokens
    val blocks = remember(text) { parseDocument(text) }
    BackHandler(onBack = onBack)
    LazyColumn(modifier.fillMaxSize().background(tokens.colors.background).safeDrawingPadding().testTag(DOCUMENT_READER_TAG)) {
        item { BackTitle(title, onBack) }
        itemsIndexed(blocks) { _, block ->
            val padding = Modifier.padding(horizontal = tokens.spacing.screenGutter, vertical = tokens.spacing.listItemCompact)
            when (block) {
                is DocBlock.Heading -> Text(
                    block.text,
                    color = tokens.colors.text,
                    fontSize = tokens.textSizes.body,
                    fontWeight = FontWeight.Bold,
                    modifier = padding.padding(top = tokens.spacing.sectionGap).semantics { heading() }
                )

                is DocBlock.Paragraph -> Text(block.text, color = tokens.colors.text, fontSize = tokens.textSizes.small, modifier = padding)

                is DocBlock.Numbered -> Text(block.text, color = tokens.colors.text, fontSize = tokens.textSizes.small, modifier = padding)

                is DocBlock.Bullet -> Row(padding, verticalAlignment = Alignment.Top) {
                    // Doto has no bullet: a drawn dot, as for the other icons.
                    DotMatrixIcon(
                        BULLET_DOT,
                        height = tokens.spacing.sectionGap,
                        contentDescription = "",
                        modifier = Modifier.padding(top = tokens.spacing.sectionGap, end = tokens.spacing.sectionGap)
                    )
                    Text(block.text, color = tokens.colors.text, fontSize = tokens.textSizes.small)
                }
            }
        }
    }
}

private val BULLET_DOT = listOf("●")

const val DOCUMENT_READER_TAG = "document_reader"
