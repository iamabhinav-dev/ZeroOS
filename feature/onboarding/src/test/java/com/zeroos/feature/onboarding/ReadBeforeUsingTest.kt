package com.zeroos.feature.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.docs.DocumentsGate
import com.zeroos.core.data.docs.DocumentsRepository
import com.zeroos.core.data.docs.ReleaseDocument
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.ui.BACK_ARROW_TAG
import com.zeroos.core.ui.ZeroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class ReadBeforeUsingScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions = ReadBeforeUsingActions(
        onToggle = { calls += "toggle:$it" },
        onOpen = { calls += "open:${it.name}" },
        onClose = { calls += "close" },
        onContinue = { calls += "continue" }
    )

    private fun show(state: ReadBeforeUsingState, changed: Boolean = false, theme: ThemeMode = ThemeMode.DARK) =
        compose.setContent { ZeroTheme(theme) { ReadBeforeUsingScreen(changed, state, actions) } }

    @Test
    fun f14_8_continueOnlyAfterTheBoxIsTicked() {
        show(ReadBeforeUsingState("0.0.30"))
        compose.onNodeWithText("Before you start").assertExists()
        compose.onNodeWithText("Read these for ZeroOS 0.0.30:").assertExists()
        compose.onNodeWithTag(ReadBeforeUsingTags.ACCEPT).assertIsOff().performClick()
        compose.onNodeWithText("Continue").assertIsNotEnabled().performClick()
        compose.onNodeWithText("Skip").assertDoesNotExist()
        assertEquals(listOf("toggle:true"), calls)
    }

    @Test
    fun f14_8_tickedLetsYouContinue() {
        show(ReadBeforeUsingState("0.0.30", accepted = true))
        compose.onNodeWithTag(ReadBeforeUsingTags.ACCEPT).assertIsOn()
        compose.onNodeWithText("Continue").assertIsEnabled().performClick()
        assertEquals(listOf("continue"), calls)
    }

    @Test
    fun f14_8_eachDocumentOpensInTheReader() {
        show(ReadBeforeUsingState("0.0.30"))
        compose.onNodeWithText("README").performClick()
        compose.onNodeWithText("License").performClick()
        compose.onNodeWithText("Third-party notices").performClick()
        assertEquals(listOf("open:README", "open:LICENSE", "open:NOTICES"), calls)
    }

    @Test
    fun f14_8_d052_readerShowsTheDocumentAndBackCloses() {
        show(ReadBeforeUsingState("0.0.30", open = ReleaseDocument.LICENSE, openText = "1. PERSONAL USE ALLOWED\nYou may use it."))
        compose.onNodeWithText("PERSONAL USE ALLOWED", substring = true).assertExists()
        compose.onNodeWithTag(BACK_ARROW_TAG).performClick()
        assertEquals(listOf("close"), calls)
    }

    @Test
    fun f14_8_afterAnUpdateTheTitleSaysTheyChanged() {
        show(ReadBeforeUsingState("0.0.31"), changed = true)
        compose.onNodeWithText("The documents changed").assertExists()
    }

    @Test
    fun f14_8_readBeforeUsingScreenshot() {
        show(ReadBeforeUsingState("0.0.30"))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f14_8_readBeforeUsingLightTickedScreenshot() {
        show(ReadBeforeUsingState("0.0.30", accepted = true), theme = ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w872dp-h392dp-land-xxhdpi")
    fun landscape_readBeforeUsingScreenshot() {
        show(ReadBeforeUsingState("0.0.30"))
        compose.onRoot().captureRoboImage()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ReadBeforeUsingViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun waitFor(condition: () -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!condition()) kotlinx.coroutines.delay(10) } }

    @Test
    fun f14_8_acceptingStoresItAndOpensTheLauncher() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val settings = SettingsRepository(store, logger)
        val docs = DocumentsRepository({ it.name.lowercase() }, settings, Dispatchers.Unconfined, logger, "0.0.30")
        val vm = ReadBeforeUsingViewModel(docs, logger, SavedStateHandle())
        waitFor { vm.gate.value == DocumentsGate.FIRST }

        vm.proceed()
        assertEquals("Continue does nothing until ticked", DocumentsGate.FIRST, vm.gate.value)

        vm.open(ReleaseDocument.LICENSE)
        waitFor { vm.state.value.openText == "license" }
        vm.close()
        assertEquals(null, vm.state.value.open)

        vm.setAccepted(true)
        vm.proceed()
        waitFor { vm.gate.value == DocumentsGate.ACCEPTED }
        assertTrue(settings.settings.first().acceptedDocuments != null)
        assertTrue(logged.contains("documents accepted for 0.0.30"))
    }

    @Test
    fun nfrR7_tickAndOpenDocumentSurviveProcessDeath() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val docs = DocumentsRepository({ it.name.lowercase() }, SettingsRepository(store, logger), Dispatchers.Unconfined, logger, "0.0.30")
        val saved = SavedStateHandle()
        ReadBeforeUsingViewModel(docs, logger, saved).apply {
            setAccepted(true)
            open(ReleaseDocument.README)
        }
        val restored = ReadBeforeUsingViewModel(docs, logger, saved)
        waitFor { restored.state.value.openText == "readme" }
        assertTrue(restored.state.value.accepted)
        assertEquals(ReleaseDocument.README, restored.state.value.open)
    }
}
