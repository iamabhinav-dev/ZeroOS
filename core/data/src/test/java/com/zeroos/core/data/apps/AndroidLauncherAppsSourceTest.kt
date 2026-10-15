package com.zeroos.core.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.model.AppKey
import com.zeroos.core.model.ProfileType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class AndroidLauncherAppsSourceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val source = AndroidLauncherAppsSource(
        launcherApps,
        userManager,
        object : com.zeroos.core.logging.Logger {
            override fun log(level: com.zeroos.core.logging.LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
        }
    )

    private fun info(pkg: String, label: String): LauncherActivityInfo = mockk {
        every { componentName } returns ComponentName(pkg, "$pkg.Main")
        every { this@mockk.label } returns label
    }

    @Test
    fun step1_2_readsLaunchableActivitiesOfMainProfile() {
        shadowOf(launcherApps).addActivity(Process.myUserHandle(), info("com.example.maps", "Maps"))

        val serial = userManager.getSerialNumberForUser(Process.myUserHandle())
        assertEquals(
            listOf(LaunchableActivity(AppKey("com.example.maps", "com.example.maps.Main", serial), "Maps", ProfileType.MAIN)),
            source.launchableActivities()
        )
    }

    @Test
    fun f2_9_reportsPackageChangesUntilUnregistered() {
        val changes = mutableListOf<String>()
        val handle = source.registerChangeListener { changes += it }

        shadowOf(launcherApps).notifyPackageAdded("com.new")
        ShadowLooper.idleMainLooper()
        handle.unregister()
        shadowOf(launcherApps).notifyPackageRemoved("com.new")
        ShadowLooper.idleMainLooper()

        assertEquals(listOf("com.new"), changes)
    }

    @Test
    fun f2_10_mapsAndroidUserTypesToProfiles() {
        assertEquals(ProfileType.WORK, profileFromUserType("android.os.usertype.profile.MANAGED"))
        assertEquals(ProfileType.PRIVATE, profileFromUserType("android.os.usertype.profile.PRIVATE"))
        assertEquals(ProfileType.CLONE, profileFromUserType("android.os.usertype.profile.CLONE"))
        assertEquals(ProfileType.CLONE, profileFromUserType(null))
    }

    @Test
    fun step1_2_launchFailsForMissingProfile() {
        val result = runCatching { source.launch(AppKey("com.x", "com.x.Main", 12345)) }
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
