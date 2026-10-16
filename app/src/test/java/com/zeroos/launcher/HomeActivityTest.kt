package com.zeroos.launcher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.data.system.isDefaultHome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * Step 1.1: ZeroOS registers as a home app. Back on home does nothing only while ZeroOS is the default
 * launcher; opened as a normal app it behaves like one (Back leaves, appears in Recents).
 */
@RunWith(AndroidJUnit4::class)
class HomeActivityTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun step1_1_resolvesHomeIntent() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(context.packageName)
        val matches = context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
        assertEquals(MainActivity::class.java.name, matches.single().activityInfo.name)
    }

    @Test
    fun step1_1_isAlsoListedAsNormalApp() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        // Debug builds also contain LeakCanary's own "Leaks" launcher entry, so check for ours specifically.
        val names = context.packageManager.queryIntentActivities(launcher, 0).map { it.activityInfo.name }
        assertTrue(MainActivity::class.java.name in names)
    }

    @Test
    fun step1_1_homeActivityLaunchFlags() {
        val info = context.packageManager.getActivityInfo(
            android.content.ComponentName(context, MainActivity::class.java),
            0
        )
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, info.launchMode)
        // Regression: excluding it from Recents blocked the system swipe-up-and-hold gesture when ZeroOS was
        // opened as a normal app. Android keeps the default home screen out of Recents by itself.
        assertFalse(info.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(info.flags and ActivityInfo.FLAG_STATE_NOT_NEEDED != 0)
        assertTrue(info.flags and ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH != 0)
    }

    private fun makeDefaultHome(packageName: String) {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val shadow = shadowOf(context.packageManager)
        shadow.removeResolveInfosForIntent(home, context.packageName)
        shadow.addResolveInfoForIntent(
            home,
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    this.packageName = packageName
                    name = "$packageName.Home"
                }
            }
        )
    }

    @Test
    fun step1_1_detectsWhetherZeroOsIsDefaultHome() {
        makeDefaultHome(context.packageName)
        assertTrue(isDefaultHome(context.packageManager, context.packageName))
        makeDefaultHome("com.example.stocklauncher")
        assertFalse(isDefaultHome(context.packageManager, context.packageName))
    }

    @Test
    fun step1_1_backOnHomeDoesNothingWhenDefaultLauncher() {
        makeDefaultHome(context.packageName)
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            controller.get().onBackPressedDispatcher.onBackPressed()
            assertFalse(controller.get().isFinishing)
        }
    }

    @Test
    fun step1_1_backLeavesWhenOpenedAsNormalApp() {
        makeDefaultHome("com.example.stocklauncher")
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            controller.get().onBackPressedDispatcher.onBackPressed()
            assertTrue(controller.get().isFinishing)
        }
    }
}
