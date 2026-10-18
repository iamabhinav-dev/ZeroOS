package com.zeroos.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.SpecialPermission
import com.zeroos.core.model.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class PermissionExplanationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions =
        PermissionActions(onOpenSettings = { calls += "settings" }, onOpenAppInfo = { calls += "app info" }, onNotNow = {
            calls +=
                "not now"
        })

    private fun show(permission: SpecialPermission, notYet: Boolean = false, theme: ThemeMode = ThemeMode.DARK) =
        compose.setContent { ZeroTheme(theme) { PermissionExplanation(permission, notYet, actions) } }

    @Test
    fun nfrS4_usageAccessExplainsWhyAndStaysLocal() {
        show(SpecialPermission.USAGE_ACCESS)
        compose.onNodeWithText("Usage access").assertExists()
        compose.onNodeWithText("It stays on this phone. Nothing is sent anywhere.").assertExists()
        compose.onNodeWithText("If Android says \"Restricted setting\":").assertDoesNotExist()
        compose.onNodeWithText("Open settings").performClick()
        compose.onNodeWithText("Not now").performClick()
        assertEquals(listOf("settings", "not now"), calls)
    }

    @Test
    fun d052_backArrowIsNotNow() {
        show(SpecialPermission.USAGE_ACCESS)
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf("not now"), calls)
    }

    @Test
    fun f14_4_accessibilityShowsRestrictedStepsAndBankingNote() {
        show(SpecialPermission.ACCESSIBILITY)
        compose.onNodeWithText("It sees which app is in front and reads nothing else.").assertExists()
        compose.onNodeWithText("Some banking and payment apps warn while any accessibility service is on.").assertExists()
        val list = compose.onNodeWithTag(PermissionTestTags.SCREEN)
        list.performScrollToNode(hasText("2 Tap the three dots (top right)"))
        compose.onNodeWithText("2 Tap the three dots (top right)").assertExists()
        list.performScrollToNode(hasText("1 Open App info for ZeroOS"))
        compose.onNodeWithText("1 Open App info for ZeroOS").performClick()
        assertEquals(listOf("app info"), calls)
    }

    @Test
    fun step4_1_notYetAfterReturningWithout() {
        show(SpecialPermission.DO_NOT_DISTURB, notYet = true)
        compose.onNodeWithText("Not allowed yet").assertExists()
        compose.onNodeWithText("Lets Focus turn on Do Not Disturb and back off.").assertExists()
    }

    @Test
    fun step4_1_requestChecksAgainWhenZeroOsIsBack() {
        var granted = false
        val results = mutableListOf<Boolean>()
        compose.setContent {
            ZeroTheme {
                PermissionRequest(SpecialPermission.USAGE_ACCESS, isGranted = { granted }, openSettings = {}, openAppInfo = {}, onResult = {
                    results +=
                        it
                })
            }
        }
        compose.onNodeWithText("Open settings").performClick()
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText("Not allowed yet").assertExists()
        assertEquals(emptyList<Boolean>(), results)

        granted = true
        compose.onNodeWithText("Open settings").performClick()
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        assertEquals(listOf(true), results)
    }

    @Test
    fun step4_1_notNowReportsFalse() {
        val results = mutableListOf<Boolean>()
        compose.setContent {
            ZeroTheme {
                PermissionRequest(SpecialPermission.USAGE_ACCESS, isGranted = { false }, openSettings = {}, openAppInfo = {}, onResult = {
                    results +=
                        it
                })
            }
        }
        compose.onNodeWithText("Not now").performClick()
        assertEquals(listOf(false), results)
    }

    @Test
    fun step4_1_usageAccessScreenshot() {
        show(SpecialPermission.USAGE_ACCESS)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_1_accessibilityScreenshot() {
        show(SpecialPermission.ACCESSIBILITY)
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_accessibilityScreenshot() {
        show(SpecialPermission.ACCESSIBILITY)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step4_1_accessibilityLightScreenshot() {
        show(SpecialPermission.ACCESSIBILITY, notYet = true, theme = ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }
}
