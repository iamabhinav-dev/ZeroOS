package com.zeroos.launcher

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Testing Policy #8: the merged manifest may only request permissions listed in the allowlist. */
@RunWith(AndroidJUnit4::class)
class PermissionAllowlistTest {

    @Test
    fun nfrS_requestedPermissionsMatchAllowlist() {
        val context: Context = ApplicationProvider.getApplicationContext()

        @Suppress("DEPRECATION")
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toSortedSet()

        assertEquals(
            "Update config/permissions-allowlist.txt with the requirement ID, or remove the permission",
            allowlist(),
            requested
        )
    }

    /** The main list, plus debug-only extras when testing the debug build. */
    private fun allowlist(): Set<String> {
        val files = listOfNotNull(
            "../config/permissions-allowlist.txt",
            "../config/permissions-allowlist-debug.txt".takeIf { BuildConfig.DEBUG }
        )
        return files.flatMap { File(it).readLines() }
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toSortedSet()
    }
}
