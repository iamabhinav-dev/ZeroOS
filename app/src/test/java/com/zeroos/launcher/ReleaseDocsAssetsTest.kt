package com.zeroos.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeroos.core.data.docs.AssetDocumentSource
import com.zeroos.core.data.docs.ReleaseDocument
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** F14.8: the APK carries this release's documents; the README part is the user section only. */
@RunWith(AndroidJUnit4::class)
class ReleaseDocsAssetsTest {
    private val source = AssetDocumentSource(ApplicationProvider.getApplicationContext<Context>().assets)
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "LICENSE").isFile }

    @Test
    fun f14_8_licenseAndNoticesAreTheRepositoryFiles() {
        assertEquals(File(root, "LICENSE").readText(), source.read(ReleaseDocument.LICENSE))
        assertEquals(File(root, "THIRD_PARTY_NOTICES.md").readText(), source.read(ReleaseDocument.NOTICES))
    }

    @Test
    fun f14_8_readmeIsTheInAppSectionOnly() {
        val readme = requireNotNull(source.read(ReleaseDocument.README))
        assertTrue(readme.contains("Before you install"))
        assertFalse("developer notes stay out", readme.contains("For developers"))
        assertFalse("markers are removed", readme.contains("in-app:"))
    }
}
