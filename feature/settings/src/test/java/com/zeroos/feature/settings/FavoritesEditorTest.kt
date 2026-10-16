package com.zeroos.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.ui.ZeroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class FavoritesEditorTest {
    private fun hasCustomActions() = androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)

    private fun hasClickLabel(label: String) = androidx.compose.ui.test.SemanticsMatcher("click label = $label") {
        it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun app(name: String) = AppEntry(AppKey("pkg.$name", "Main", 0), name)
    private val favorites = listOf("Phone", "Messages", "Camera", "Maps").map(::app)
    private val calls = mutableListOf<String>()

    private fun showEditor(state: FavoritesEditorState) = compose.setContent {
        ZeroTheme {
            FavoritesEditorScreen(
                state = state,
                onRemove = { calls += "remove:${it.label}" },
                onMove = { from, to -> calls += "move:$from->$to" },
                onAdd = { calls += "add" },
                onBack = { calls += "back" }
            )
        }
    }

    @Test
    fun f3_3_showsCountAgainstMaximum() {
        showEditor(FavoritesEditorState(favorites, max = 6))
        compose.onNodeWithText("4 / 6").assertExists()
        compose.onNodeWithText("+ Add app").performClick()
        assertEquals(listOf("add"), calls)
    }

    @Test
    fun f3_3_addHiddenWhenFull() {
        showEditor(FavoritesEditorState(favorites, max = 4))
        compose.onNodeWithText("+ Add app").assertDoesNotExist()
    }

    @Test
    fun f3_1_removeWithTimes() {
        showEditor(FavoritesEditorState(favorites, max = 6))
        compose.onNode(hasClickLabel("Remove Camera")).performClick()
        assertEquals(listOf("remove:Camera"), calls)
    }

    @Test
    fun f3_2_dragHandleReorders() {
        showEditor(FavoritesEditorState(favorites, max = 6))
        compose.onNodeWithTag(FavoritesTestTags.handle(0), useUnmergedTree = true).performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 48.dp.toPx() * 2.5f)
        }
        assertEquals(listOf("move:0->2"), calls)
    }

    @Test
    fun f3_2_nfrA1_moveActionsForScreenReaders() {
        showEditor(FavoritesEditorState(favorites, max = 6))
        val row = compose.onNode(hasCustomActions() and androidx.compose.ui.test.hasAnyDescendant(hasText("Messages")))
        val actions = row.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Move up", "Move down"), actions.map { it.label })
        actions.first().action()
        assertEquals(listOf("move:1->0"), calls)
    }

    @Test
    fun f3_1_pickerSearchesAndPicks() {
        compose.setContent {
            ZeroTheme {
                AppPickerScreen(
                    state = FavoritesEditorState(favorites, 6, candidates = listOf(app("Gmail"), app("Google Maps")), query = ""),
                    onQueryChange = { calls += "query:$it" },
                    onPick = { calls += "pick:${it.label}" },
                    onBack = { calls += "back" }
                )
            }
        }
        compose.onNodeWithText("search…").performTextInput("gm")
        compose.onNodeWithText("Gmail").performClick()
        assertEquals(listOf("query:gm", "pick:Gmail"), calls)
    }

    @Test
    fun step2_5_editorScreenshot() {
        showEditor(FavoritesEditorState(favorites, max = 6))
        compose.onRoot().captureRoboImage()
    }
}
