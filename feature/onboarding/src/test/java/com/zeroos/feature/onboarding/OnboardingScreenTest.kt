package com.zeroos.feature.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.zeroos.core.model.AppEntry
import com.zeroos.core.model.AppKey
import com.zeroos.core.ui.ZeroTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h872dp-xxhdpi")
class OnboardingScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private fun app(name: String) = AppEntry(AppKey("pkg.$name", "Main", 0), name)
    private val apps = listOf("Phone", "Messages", "Camera", "Maps", "Calculator", "Clock").map(::app)

    private fun show(state: OnboardingState) = compose.setContent {
        ZeroTheme {
            OnboardingScreen(
                state,
                OnboardingActions(
                    onNext = { calls += "next" },
                    onBack = { calls += "back" },
                    onSkip = { calls += "skip" },
                    onToggle = { calls += "toggle:${it.label}" },
                    onSetDefault = { calls += "default" },
                    onTurnOffBatteryOptimization = { calls += "battery" }
                )
            )
        }
    }

    private val favorites = OnboardingState(
        page = OnboardingPage.FAVORITES,
        apps = apps,
        selected = apps.take(3).map { it.key.asString() },
        maxFavorites = 6
    )

    @Test
    fun f14_1_f14_5_introHasNextAndSkip() {
        show(OnboardingState())
        compose.onNodeWithText("Less phone.\nMore life.").assertExists()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Skip").performClick()
        assertEquals(listOf("next", "skip"), calls)
    }

    @Test
    fun f14_2_f18_7_setAsDefaultOrLater() {
        show(OnboardingState(page = OnboardingPage.DEFAULT_HOME))
        compose.onNodeWithText("Keep your phone's own launcher installed: it's your fallback if anything goes wrong.").assertExists()
        compose.onNodeWithText("Set as default").performClick()
        compose.onNodeWithText("Later").performClick()
        assertEquals(listOf("default", "next"), calls)
    }

    @Test
    fun f14_2_alreadyDefaultJustContinues() {
        show(OnboardingState(page = OnboardingPage.DEFAULT_HOME, isDefaultHome = true))
        compose.onNodeWithText("ZeroOS is your home screen").assertExists()
        compose.onNodeWithText("Set as default").assertDoesNotExist()
    }

    @Test
    fun f14_3_picksShowOrderAndToggle() {
        show(favorites)
        compose.onNodeWithText("3 / 6").assertExists()
        compose.onNodeWithText("Phone").assertIsOn()
        compose.onNodeWithText("Clock").assertIsOff().performClick()
        assertEquals(listOf("toggle:Clock"), calls)
    }

    @Test
    fun f14_6_offersBatteryDialogOnlyWhenOptimized() {
        show(OnboardingState(page = OnboardingPage.BACKGROUND, batteryOptimized = true))
        compose.onNodeWithText("Battery optimization: on").assertExists()
        compose.onNodeWithText("Skip").assertDoesNotExist()
        compose.onNodeWithText("Turn off for ZeroOS").performClick()
        compose.onNodeWithText("Done").performClick()
        assertEquals(listOf("battery", "next"), calls)
    }

    @Test
    fun f14_6_noButtonWhenAlreadyAllowed() {
        show(OnboardingState(page = OnboardingPage.BACKGROUND, batteryOptimized = false))
        compose.onNodeWithText("Battery optimization: off").assertExists()
        compose.onNodeWithText("Turn off for ZeroOS").assertDoesNotExist()
    }

    @Test
    fun step2_9_introScreenshot() {
        show(OnboardingState())
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_9_defaultHomeScreenshot() {
        show(OnboardingState(page = OnboardingPage.DEFAULT_HOME))
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_9_favoritesScreenshot() {
        show(favorites)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun step2_9_backgroundScreenshot() {
        show(OnboardingState(page = OnboardingPage.BACKGROUND, batteryOptimized = true))
        compose.onRoot().captureRoboImage()
    }
}

@RunWith(AndroidJUnit4::class)
class AndroidOnboardingSystemTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val system = AndroidOnboardingSystem(context)

    private fun handle(pkg: String, filter: IntentFilter) {
        val component = ComponentName(pkg, "$pkg.Main")
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(component)
            addIntentFilterForActivity(component, filter.apply { addCategory(Intent.CATEGORY_DEFAULT) })
        }
    }

    @Test
    fun f14_3_suggestsDefaultApps() {
        handle("com.dialer", IntentFilter(Intent.ACTION_DIAL))
        handle("com.maps", IntentFilter(Intent.ACTION_VIEW).apply { addDataScheme("geo") })
        assertEquals(listOf("com.dialer", "com.maps"), system.suggestedPackages())
    }

    @Test
    fun f14_6_detectsBatteryOptimization() {
        val power = shadowOf(context.getSystemService(PowerManager::class.java))
        power.setIgnoringBatteryOptimizations(context.packageName, false)
        assertTrue(system.isBatteryOptimized())
        power.setIgnoringBatteryOptimizations(context.packageName, true)
        assertFalse(system.isBatteryOptimized())
    }

    @Test
    fun f14_2_notDefaultHomeByDefault() {
        assertFalse(system.isDefaultHome())
    }
}
