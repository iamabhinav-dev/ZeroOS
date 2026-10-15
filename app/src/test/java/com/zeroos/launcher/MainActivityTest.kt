package com.zeroos.launcher

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.feature.applist.AppListTestTags
import com.zeroos.feature.home.HomeTestTags
import com.zeroos.launcher.diagnostics.DiagnosticsTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Step 1.3 end to end: home → swipe up → app list → Back / Home press → home. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun openList() {
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { swipeUp() }
        compose.onNodeWithTag(AppListTestTags.LIST).assertExists()
    }

    @Test
    fun step1_3_startsOnHome() {
        compose.onNodeWithTag(HomeTestTags.ROOT).assertExists()
    }

    @Test
    fun f2_11_backFromListReturnsHome() {
        openList()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag(HomeTestTags.ROOT).assertExists()
    }

    @Test
    fun f1_9_longPressOpensDiagnosticsAndBackReturnsHome() {
        compose.onNodeWithTag(HomeTestTags.ROOT).performTouchInput { longClick(center) }
        compose.onNodeWithTag(DiagnosticsTestTags.ROOT).assertExists()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag(HomeTestTags.ROOT).assertExists()
    }

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
