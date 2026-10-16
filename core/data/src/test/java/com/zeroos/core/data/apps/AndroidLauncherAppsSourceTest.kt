package com.zeroos.core.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
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
        context,
        launcherApps,
        userManager,
        object : com.zeroos.core.logging.Logger {
            override fun log(level: com.zeroos.core.logging.LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
        }
    )

    private fun info(pkg: String, label: String, system: Boolean = false): LauncherActivityInfo = mockk {
        every { componentName } returns ComponentName(pkg, "$pkg.Main")
        every { this@mockk.label } returns label
        every { applicationInfo } returns ApplicationInfo().apply { flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0 }
    }

    @Test
    fun f2_8_systemAppsAreNotRemovable() {
        shadowOf(launcherApps).addActivity(Process.myUserHandle(), info("com.android.contacts", "Contacts", system = true))
        shadowOf(launcherApps).addActivity(Process.myUserHandle(), info("com.instagram.android", "Instagram"))
        assertEquals(listOf(false, true), source.launchableActivities().map { it.removable })
    }

    @Test
    fun f2_8_uninstallOpensAndroidsDialogForTheAppsProfile() {
        val user = Process.myUserHandle()
        source.uninstall(AppKey("com.instagram.android", "com.instagram.android.Main", userManager.getSerialNumberForUser(user)))
        val intent = shadowOf(context as android.app.Application).nextStartedActivity
        assertEquals(Intent.ACTION_DELETE, intent.action)
        assertEquals("package:com.instagram.android", intent.dataString)
        assertEquals(user, intent.getParcelableExtra(Intent.EXTRA_USER, android.os.UserHandle::class.java))
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
