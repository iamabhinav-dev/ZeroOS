package com.zeroos.feature.settings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.backup.BackupRepository
import com.zeroos.core.data.backup.SettingsBackup
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.DiagnosticsSettings
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.testing.MutableClock
import com.zeroos.core.ui.ZeroTheme
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.TimeZone
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
import org.junit.Assert.assertNotNull
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
class BackupScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun show(state: BackupState) = compose.setContent {
        ZeroTheme {
            BackupScreen(
                state = state,
                onExport = { calls += "export" },
                onImport = { calls += "import" },
                onConfirmImport = { calls += "confirm" },
                onCancelImport = { calls += "cancel" },
                onBack = {}
            )
        }
    }

    @Test
    fun f13_rowsAndNeverExported() {
        show(BackupState())
        compose.onNodeWithText("Not exported yet").assertExists()
        compose.onNodeWithText("Export settings").performClick()
        compose.onNodeWithText("Import settings").performClick()
        assertEquals(listOf("export", "import"), calls)
    }

    @Test
    fun f13_2_confirmBeforeReplacing() {
        show(BackupState(pending = SettingsBackup()))
        compose.onNodeWithText("Replace all settings with this backup?").assertExists()
        compose.onNodeWithText("Import").performClick()
        assertEquals(listOf("confirm"), calls)
    }

    @Test
    fun f13_5_explainsRejectedFiles() {
        show(BackupState(message = BackupMessage.TooNew(3)))
        compose.onNodeWithText("This backup is from a newer ZeroOS (version 3)").assertExists()
    }

    @Test
    fun step2_10_backupScreenshot() {
        val zone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            show(BackupState(lastExportMillis = 1_792_041_120_000L, message = BackupMessage.Exported))
            compose.onRoot().captureRoboImage()
        } finally {
            TimeZone.setDefault(zone)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BackupViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun waitFor(condition: () -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!condition()) kotlinx.coroutines.delay(10) } }

    @Test
    fun f13_1_f13_2_exportReadConfirm() = runTest {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val settings = SettingsRepository(store, logger)
        val diagnostics = DiagnosticsSettings(context.getSharedPreferences("bvm", Context.MODE_PRIVATE), LogLevel.INFO)
        val repo =
            BackupRepository(
                settings,
                diagnostics,
                context.getSharedPreferences("bvm-backup", Context.MODE_PRIVATE),
                MutableClock(),
                logger
            )
        val vm = BackupViewModel(repo, logger, Dispatchers.IO)

        settings.update("test") { it.copy(favorites = listOf("a/a.Main#0")) }
        val out = ByteArrayOutputStream()
        vm.export { out }
        waitFor { vm.state.value.message == BackupMessage.Exported }
        assertNotNull(vm.state.value.lastExportMillis)

        settings.update("test") { it.copy(favorites = emptyList()) }
        vm.read { ByteArrayInputStream(out.toByteArray()) }
        waitFor { vm.state.value.pending != null }
        vm.confirmImport()
        waitFor { vm.state.value.message == BackupMessage.Imported }
        assertEquals(listOf("a/a.Main#0"), store.data.first().favorites)

        vm.read { ByteArrayInputStream("nope".toByteArray()) }
        waitFor { vm.state.value.message == BackupMessage.NotABackup }
        vm.read { null }
        waitFor { vm.state.value.message == BackupMessage.Failed }
    }
}
