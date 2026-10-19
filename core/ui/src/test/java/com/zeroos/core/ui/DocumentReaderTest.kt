package com.zeroos.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

class DocumentParserTest {
    @Test
    fun f14_8_markdownBecomesHeadingsParagraphsAndBullets() {
        val text = """
            # Title
            <!-- hidden -->
            First line of a
            wrapped **paragraph** with a [link](https://example.invalid).

            - one `item`
              that wraps
            - two

            1. First step
               continues
            2. Second step

            | Document | What |
            |---|---|
            | README | Notes |

            ```
            ./gradlew check
            ```
        """.trimIndent()
        assertEquals(
            listOf(
                DocBlock.Heading("Title"),
                DocBlock.Paragraph("First line of a wrapped paragraph with a link."),
                DocBlock.Bullet("one item that wraps"),
                DocBlock.Bullet("two"),
                DocBlock.Numbered("1. First step continues"),
                DocBlock.Numbered("2. Second step"),
                DocBlock.Paragraph("Document — What"),
                DocBlock.Paragraph("README — Notes")
            ),
            parseDocument(text)
        )
    }

    @Test
    fun f14_8_plainLicenceTitlesAreHeadings() {
        val text = "ZeroOS Licence\n\n1. PERSONAL USE ALLOWED\n   You may use it\n   on your phone.\n\nTHE SOFTWARE IS PROVIDED"
        assertEquals(
            listOf(
                DocBlock.Paragraph("ZeroOS Licence"),
                DocBlock.Heading("1. PERSONAL USE ALLOWED"),
                DocBlock.Paragraph("You may use it on your phone."),
                DocBlock.Heading("THE SOFTWARE IS PROVIDED")
            ),
            parseDocument(text)
        )
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class DocumentReaderTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val sample = """
        ## Before you install — read this
        - Read this README, the LICENSE and the third-party notices first.
        - **Use at your own risk.** ZeroOS replaces your home screen and may cause problems.

        ## Install
        1. Use a phone with Android 8.0 or newer.
        2. Download the APK and open it.
    """.trimIndent()

    private fun show(theme: ThemeMode = ThemeMode.DARK) = compose.setContent {
        ZeroTheme(theme) { DocumentReader("README", sample, onBack = {}) }
    }

    @Test
    fun f14_8_showsTheDocumentWithItsTitle() {
        show()
        compose.onNodeWithText("README").assertExists()
        compose.onNodeWithText("Install").assertExists()
        compose.onNodeWithText("2. Download the APK and open it.").assertExists()
    }

    @Test
    fun f14_8_readerScreenshot() {
        show()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f14_8_readerLightScreenshot() {
        show(ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_readerScreenshot() {
        show()
        compose.onRoot().captureRoboImage()
    }
}
