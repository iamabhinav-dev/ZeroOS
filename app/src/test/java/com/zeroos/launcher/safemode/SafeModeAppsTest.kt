package com.zeroos.launcher.safemode

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SafeModeAppsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    private fun info(pkg: String, label: String): LauncherActivityInfo = mockk {
        every { componentName } returns ComponentName(pkg, "$pkg.Main")
        every { this@mockk.label } returns label
    }

    @Test
    fun f18_3_listsMainProfileAppsAlphabetically() {
        shadowOf(launcherApps).addActivity(Process.myUserHandle(), info("b", "bravo"))
        shadowOf(launcherApps).addActivity(Process.myUserHandle(), info("a", "Alpha"))
        assertEquals(listOf("Alpha", "bravo"), SafeModeApps(launcherApps).load().map { it.label })
    }
}
