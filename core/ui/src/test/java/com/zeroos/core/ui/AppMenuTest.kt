package com.zeroos.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppMenuTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions = AppMenuActions(
        onToggleFavorite = { calls += "favorite" },
        onRename = { calls += "rename:$it" },
        onHide = { calls += "hide" },
        onAppInfo = { calls += "info" },
        onUninstall = { calls += "uninstall" }
    )

    private fun show(target: AppMenuTarget) = compose.setContent {
        ZeroTheme { AppMenu(target, actions, onDismiss = { calls += "dismiss" }) }
    }

    @Test
    fun f2_8_menuShowsAllActions() {
        show(AppMenuTarget("Instagram", isFavorite = false, isRenamed = false))
        listOf("Instagram", "Add to favorites", "Rename", "Hide", "App info", "Uninstall").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText("Hide").performClick()
        assertEquals(listOf("hide", "dismiss"), calls)
    }

    @Test
    fun f2_8_favoriteRowSaysRemoveWhenAlreadyFavorite() {
        show(AppMenuTarget("Maps", isFavorite = true, isRenamed = false))
        compose.onNodeWithText("Remove from favorites").performClick()
        assertEquals(listOf("favorite", "dismiss"), calls)
    }

    @Test
    fun f2_8_appInfoAndUninstall() {
        show(AppMenuTarget("Maps", isFavorite = false, isRenamed = false))
        compose.onNodeWithText("App info").assertHasClickAction().performClick()
        assertEquals(listOf("info", "dismiss"), calls)
    }

    @Test
    fun f2_8_uninstallCallsAction() {
        show(AppMenuTarget("Instagram", isFavorite = false, isRenamed = false))
        compose.onNodeWithText("Uninstall").performClick()
        assertEquals(listOf("uninstall", "dismiss"), calls)
    }

    @Test
    fun f2_8_systemAppsHaveNoUninstall() {
        show(AppMenuTarget("Contacts", isFavorite = false, isRenamed = false, canUninstall = false))
        compose.onNodeWithText("App info").assertExists()
        compose.onNodeWithText("Uninstall").assertDoesNotExist()
    }

    @Test
    fun f5_4_renameSavesNewName() {
        show(AppMenuTarget("Instagram", isFavorite = false, isRenamed = false))
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithText("Rename Instagram").assertExists()
        compose.onNodeWithText("Reset to original").assertDoesNotExist()
        compose.onNodeWithTag(AppMenuTestTags.RENAME_FIELD).performTextClearance()
        compose.onNodeWithText("Save").performClick()
        assertEquals("empty name can't be saved", emptyList<String>(), calls)
        compose.onNodeWithTag(AppMenuTestTags.RENAME_FIELD).performTextInput("Time Sink")
        compose.onNodeWithText("Save").performClick()
        assertEquals(listOf("rename:Time Sink", "dismiss"), calls)
    }

    @Test
    fun f5_5_resetOfferedForRenamedApps() {
        show(AppMenuTarget("Time Sink", isFavorite = false, isRenamed = true))
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithText("Reset to original").performClick()
        assertEquals(listOf("rename:null", "dismiss"), calls)
    }
}
