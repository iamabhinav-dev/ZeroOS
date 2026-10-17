package com.zeroos.feature.settings

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.data.wallpaper.PhoneWallpapers
import com.zeroos.core.data.wallpaper.WallpaperCatalog
import com.zeroos.core.data.wallpaper.WallpaperItem
import com.zeroos.core.data.wallpaper.WallpaperResult
import com.zeroos.core.data.wallpaper.WallpaperSystem
import com.zeroos.core.data.wallpaper.WallpaperTone
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.core.model.settings.PhoneWallpaperHistory
import com.zeroos.core.model.settings.ThemeMode
import com.zeroos.core.model.settings.WallpaperScreen
import com.zeroos.core.ui.ZeroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val files = listOf("rose_dark.webp", "wave_dark.webp", "leaf_dark.webp", "rose_light.webp", "wave_light.webp")

/** A stand-in thumbnail: the background colour with a few dots, so screenshots show the layout. */
private fun fakeThumbnail(item: WallpaperItem, width: Int, height: Int): ImageBitmap {
    val dark = item.tone == WallpaperTone.DARK
    val image = ImageBitmap(width, height)
    val canvas = Canvas(image)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = if (dark) Color.Black else Color.White })
    if (item.id != WallpaperCatalog.PURE_BLACK && item.id != WallpaperCatalog.PURE_WHITE) {
        val dot = Paint().apply { color = if (dark) Color.White else Color.Black }
        repeat(7) { canvas.drawCircle(Offset(width * (0.3f + it * 0.07f), height * (0.45f + (it % 3) * 0.04f)), width * 0.03f, dot) }
    }
    return image
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class PhoneWallpaperScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private fun gallery(tone: WallpaperTone) = WallpaperCatalog.forTone(files, tone)

    private fun show(state: PhoneWallpaperState, theme: ThemeMode = ThemeMode.DARK) = compose.setContent {
        ZeroTheme(theme) {
            PhoneWallpaperScreen(
                state = state,
                actions = PhoneWallpaperActions(
                    onSelect = { calls += "select:${it.id}" },
                    onChooseScreen = { calls += "choose:$it" },
                    onConfirm = { calls += "both:$it" },
                    onCancelAsk = { calls += "cancel" },
                    onRestore = { calls += "restore:$it" }
                ),
                thumbnail = { item, w, h -> fakeThumbnail(item, w, h) },
                onBack = {}
            )
        }
    }

    @Test
    fun f6_8_galleryShowsLabelsAndSelection() {
        show(PhoneWallpaperState(items = gallery(WallpaperTone.DARK), selectedId = "rose_dark"))
        // Labels come from strings by name ("wave" is labelled "Great wave"), Pure black first.
        listOf("Pure black", "Leaf", "Rose", "Great wave").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithTag(SettingsTestTags.wallpaper("rose_dark")).assertIsSelected()
        compose.onNodeWithText("Leaf").performClick()
        assertEquals(listOf("select:leaf_dark"), calls)
    }

    @Test
    fun f6_8_statusAndScreensToSet() {
        show(PhoneWallpaperState(items = gallery(WallpaperTone.DARK), history = PhoneWallpaperHistory(home = listOf("rose_dark"))))
        compose.onNodeWithText("Home: Rose · Lock: not set by ZeroOS").assertExists()
        compose.onNodeWithText("Set on home screen").performClick()
        compose.onNodeWithText("Set on lock screen").performClick()
        assertEquals(listOf("choose:HOME", "choose:LOCK"), calls)
    }

    @Test
    fun f6_8_alwaysAsksAboutTheOtherScreen() {
        show(PhoneWallpaperState(items = gallery(WallpaperTone.DARK), asking = WallpaperScreen.HOME))
        compose.onNodeWithText("Also use it on the lock screen?").assertExists()
        compose.onNodeWithText("Only home").performClick()
        compose.onNodeWithText("Both").performClick()
        assertEquals(listOf("both:false", "both:true"), calls)
    }

    @Test
    fun f6_8_askFromLockScreenNamesHome() {
        show(PhoneWallpaperState(items = gallery(WallpaperTone.DARK), asking = WallpaperScreen.LOCK))
        compose.onNodeWithText("Also use it on the home screen?").assertExists()
        compose.onNodeWithText("Only lock").assertExists()
    }

    @Test
    fun f6_8_restoreOnlyWhenThereIsAPreviousOne() {
        show(
            PhoneWallpaperState(
                items = gallery(WallpaperTone.DARK),
                history = PhoneWallpaperHistory(home = listOf("rose_dark", "leaf_dark"), lock = listOf("rose_dark"))
            )
        )
        compose.onNodeWithText("Restore previous lock wallpaper").assertDoesNotExist()
        compose.onNodeWithText("Restore previous home wallpaper").performClick()
        assertEquals(listOf("restore:HOME"), calls)
        compose.onNodeWithText(
            "Android doesn't let apps read the current wallpaper, so ZeroOS can only restore wallpapers it set."
        ).assertExists()
    }

    @Test
    fun f6_8_resultLines() {
        val both = WallpaperOutcome.Set(WallpaperResult(accepted = WallpaperScreen.entries.toSet(), homeBackgroundSwitched = true))
        show(PhoneWallpaperState(items = gallery(WallpaperTone.DARK), result = both))
        compose.onNodeWithText("Wallpaper set on the home and lock screens").assertExists()
        compose.onNodeWithText("If the lock screen didn't change, set it in your phone's Themes app").assertExists()
        compose.onNodeWithText("Home background now shows the phone wallpaper").assertExists()
    }

    @Test
    fun f6_8_rejectedLockAndFailuresAreExplained() {
        val rejected = WallpaperResult(accepted = setOf(WallpaperScreen.HOME), rejected = setOf(WallpaperScreen.LOCK))
        assertEquals(
            listOf(
                com.zeroos.core.resources.R.string.phone_wallpaper_done_home,
                com.zeroos.core.resources.R.string.phone_wallpaper_lock_rejected
            ),
            outcomeLines(WallpaperOutcome.Set(rejected))
        )
        assertEquals(
            listOf(com.zeroos.core.resources.R.string.phone_wallpaper_failed),
            outcomeLines(WallpaperOutcome.Set(WallpaperResult(failed = true)))
        )
        assertEquals(listOf(com.zeroos.core.resources.R.string.phone_wallpaper_gone), outcomeLines(WallpaperOutcome.Gone))
    }

    @Test
    fun f6_8_phoneWallpaperDarkScreenshot() {
        show(
            PhoneWallpaperState(
                items = gallery(WallpaperTone.DARK),
                selectedId = "rose_dark",
                history = PhoneWallpaperHistory(home = listOf("rose_dark"))
            )
        )
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun f6_8_phoneWallpaperLightScreenshot() {
        show(PhoneWallpaperState(items = gallery(WallpaperTone.LIGHT), selectedId = "pure_white"), ThemeMode.LIGHT)
        compose.onRoot().captureRoboImage()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneWallpaperViewModelTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    private class FakeSystem : WallpaperSystem {
        val set = mutableListOf<Pair<String, Set<WallpaperScreen>>>()
        var available = files

        override fun files() = available

        override fun thumbnail(item: WallpaperItem, width: Int, height: Int): Bitmap = error("not used")

        override fun set(item: WallpaperItem, screens: Set<WallpaperScreen>): Set<WallpaperScreen> {
            set += item.id to screens
            return screens
        }
    }

    private val system = FakeSystem()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun kotlinx.coroutines.test.TestScope.viewModel(): PhoneWallpaperViewModel {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val settings = SettingsRepository(store, logger)
        val vm =
            PhoneWallpaperViewModel(
                PhoneWallpapers(settings, system, UnconfinedTestDispatcher(testScheduler), logger),
                settings,
                SavedStateHandle()
            )
        backgroundScope.launch { vm.state.collect {} }
        return vm
    }

    @Test
    fun f6_8_galleryFollowsThemeAndSelectionDefaultsToTheSolid() = runTest {
        val vm = viewModel()
        vm.setTone(WallpaperTone.LIGHT)
        val state = vm.state.first { it.items.isNotEmpty() }
        assertEquals(listOf("pure_white", "rose_light", "wave_light"), state.items.map { it.id })
        assertEquals("pure_white", state.selected?.id)
    }

    @Test
    fun f6_8_onlyHomeLeavesTheLockScreenAlone() = runTest {
        val vm = viewModel()
        vm.setTone(WallpaperTone.DARK)
        vm.select(vm.state.first { it.items.isNotEmpty() }.items.first { it.id == "rose_dark" })
        vm.chooseScreen(WallpaperScreen.HOME)
        assertEquals(WallpaperScreen.HOME, vm.state.first { it.asking != null }.asking)
        vm.confirm(both = false)
        val done = vm.state.first { it.result != null }
        assertEquals(listOf("rose_dark" to setOf(WallpaperScreen.HOME)), system.set)
        assertEquals(null, done.asking)
        assertEquals("rose_dark", done.history.current(WallpaperScreen.HOME))
    }

    @Test
    fun f6_8_bothSetsBothScreens() = runTest {
        val vm = viewModel()
        vm.setTone(WallpaperTone.DARK)
        vm.state.first { it.items.isNotEmpty() }
        vm.chooseScreen(WallpaperScreen.LOCK)
        vm.confirm(both = true)
        vm.state.first { it.result != null }
        assertEquals(listOf("pure_black" to WallpaperScreen.entries.toSet()), system.set)
    }

    @Test
    fun f6_8_restorePreviousAndGone() = runTest {
        val vm = viewModel()
        vm.setTone(WallpaperTone.DARK)
        val items = vm.state.first { it.items.isNotEmpty() }.items
        listOf("leaf_dark", "rose_dark").forEach { id ->
            vm.select(items.first { it.id == id })
            vm.chooseScreen(WallpaperScreen.HOME)
            vm.confirm(both = false)
            vm.state.first { it.history.current(WallpaperScreen.HOME) == id }
        }
        system.set.clear()
        vm.restorePrevious(WallpaperScreen.HOME)
        vm.state.first { it.history.current(WallpaperScreen.HOME) == "leaf_dark" }
        assertEquals(listOf("leaf_dark" to setOf(WallpaperScreen.HOME)), system.set)

        // The previous image (rose) is removed from the gallery: the screen says so.
        system.available = files - "rose_dark.webp"
        vm.restorePrevious(WallpaperScreen.HOME)
        assertEquals(WallpaperOutcome.Gone, vm.state.first { it.result == WallpaperOutcome.Gone }.result)
    }
}
