package com.zeroos.launcher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** Step 1.1: ZeroOS registers as a home app and Back on home does nothing. */
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
        assertTrue(info.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(info.flags and ActivityInfo.FLAG_STATE_NOT_NEEDED != 0)
        assertTrue(info.flags and ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH != 0)
    }

    @Test
    fun step1_1_backOnHomeDoesNothing() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            controller.get().onBackPressedDispatcher.onBackPressed()
            assertFalse(controller.get().isFinishing)
        }
    }
}
