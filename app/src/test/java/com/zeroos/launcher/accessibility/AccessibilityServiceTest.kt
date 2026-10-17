package com.zeroos.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import com.zeroos.launcher.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class AccessibilityServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val logged = mutableListOf<String>()
    private val bridge = AccessibilityBridge(
        object : Logger {
            override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
                logged += message
            }
        }
    )

    @Test
    fun step4_2_nothingHappensWhileTheServiceIsOff() {
        assertFalse(bridge.running.value)
        assertFalse(bridge.openNotificationShade())
        assertFalse(bridge.lockScreen())
        assertTrue("notification shade: service not running" in logged)
    }

    @Test
    fun f4_4_f4_5_shadeAndLockUseGlobalActions() {
        val service = mockk<AccessibilityService> { every { performGlobalAction(any()) } returns true }
        bridge.connected(service)
        assertTrue(bridge.running.value)
        assertTrue(bridge.openNotificationShade())
        assertTrue(bridge.lockScreen())
        verify { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) }
        verify { service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) }
        bridge.disconnected(service)
        assertFalse(bridge.running.value)
    }

    @Test
    fun step4_2_onlyTheSystemCanBindAndItReadsNoScreenContent() {
        val services = context.packageManager.queryIntentServices(
            Intent(AccessibilityService.SERVICE_INTERFACE).setPackage(context.packageName),
            PackageManager.GET_META_DATA
        )
        val info = services.single().serviceInfo
        assertEquals(ZeroAccessibilityService::class.java.name, info.name)
        assertEquals(android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE, info.permission)

        val config = context.resources.getXml(R.xml.accessibility_service)
        while (config.next() != XmlPullParser.START_TAG) Unit
        val android = "http://schemas.android.com/apk/res/android"
        assertEquals("false", config.getAttributeValue(android, "canRetrieveWindowContent"))
        assertEquals(context.packageName, config.getAttributeValue(android, "packageNames"))
    }
}
