package com.zeroos.launcher

import android.content.Intent
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.data.docs.AssetDocumentSource
import com.zeroos.core.data.docs.ReleaseDocument
import com.zeroos.core.data.docs.fingerprintOf
import com.zeroos.core.data.settings.SETTINGS_FILE_NAME
import com.zeroos.feature.applist.AppListTestTags
import com.zeroos.feature.home.HomeTestTags
import com.zeroos.feature.onboarding.OnboardingTestTags
import com.zeroos.feature.onboarding.ReadBeforeUsingTags
import com.zeroos.feature.settings.SettingsTestTags
import com.zeroos.launcher.diagnostics.DiagnosticsTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Step 1.3 end to end: home → swipe up → app list → Back / Home press → home. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    val compose = createAndroidComposeRule<MainActivity>()

    /** These tests start after onboarding (F14); OnboardingGateTest covers the first start. */
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(OnboardingDoneRule()).around(compose)

    /** Home appears once settings have loaded (onboarding gate, F14). */
    @Before
    fun waitForHome() = compose.waitUntil(WAIT_MS) {
        compose.onAllNodesWithTag(HomeTestTags.ROOT).fetchSemanticsNodes().isNotEmpty()
    }

    private fun openList() {
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { swipeUp() }
        compose.onNodeWithTag(AppListTestTags.LIST).assertExists()
    }

    @Test
    fun step1_3_startsOnHome() {
        compose.onNodeWithTag(HomeTestTags.ROOT).assertExists()
    }

    @Test
    fun f4_4_swipeDownOpensSearch() {
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { swipeDown() }
        compose.onNodeWithTag(AppListTestTags.LIST).assertExists()
    }

    @Test
    fun f2_11_backFromListReturnsHome() {
        openList()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag(HomeTestTags.ROOT).assertExists()
    }

    @Test
    fun f1_9_longPressOpensSettingsAndBackReturnsHome() {
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { longClick(center) }
        compose.onNodeWithTag(SettingsTestTags.MAIN).assertExists()
        compose.onNodeWithText("Diagnostics").performClick()
        compose.onNodeWithTag(DiagnosticsTestTags.ROOT).assertExists()
        back()
        compose.onNodeWithTag(SettingsTestTags.MAIN).assertExists()
        back()
        compose.onNodeWithTag(HomeTestTags.ROOT).assertExists()
    }

    private fun back() = compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun step1_1_homePressClosesList() {
        openList()
        compose.runOnUiThread {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            // onNewIntent is protected; call it the way Android does when Home is pressed in front.
            MainActivity::class.java.getDeclaredMethod("onNewIntent", Intent::class.java)
                .apply { isAccessible = true }
                .invoke(compose.activity, home)
        }
        compose.onNodeWithTag(HomeTestTags.ROOT).assertExists()
    }
}

private const val WAIT_MS = 5_000L

/** Writes settings with the documents accepted (F14.8) and onboarding finished before the activity starts. */
class OnboardingDoneRule : org.junit.rules.ExternalResource() {
    override fun before() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = AssetDocumentSource(context.assets)
        val accepted = requireNotNull(fingerprintOf(ReleaseDocument.entries.map(source::read))) { "release documents missing from assets" }
        java.io.File(context.filesDir, "datastore/$SETTINGS_FILE_NAME").apply {
            parentFile?.mkdirs()
            writeText("""{"onboardingCompleted":true,"acceptedDocuments":"$accepted"}""")
        }
    }
}

/** First start: the documents page (F14.8), then onboarding; skipping it lands on home (F14.5). */
@RunWith(AndroidJUnit4::class)
class OnboardingGateTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun f14_8_f14_5_firstStartShowsDocumentsThenOnboardingThenHome() {
        compose.waitUntil(WAIT_MS) { compose.onAllNodesWithTag(ReadBeforeUsingTags.ROOT).fetchSemanticsNodes().isNotEmpty() }
        // Continue does nothing until the box is ticked; there's no Skip on this page.
        compose.onNodeWithText("Continue").assertIsNotEnabled()
        compose.onNodeWithText("Skip").assertDoesNotExist()
        compose.onNodeWithTag(ReadBeforeUsingTags.ACCEPT).performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.waitUntil(WAIT_MS) { compose.onAllNodesWithTag(OnboardingTestTags.ROOT).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Skip").performClick()
        compose.waitUntil(WAIT_MS) { compose.onAllNodesWithTag(HomeTestTags.ROOT).fetchSemanticsNodes().isNotEmpty() }
    }
}
