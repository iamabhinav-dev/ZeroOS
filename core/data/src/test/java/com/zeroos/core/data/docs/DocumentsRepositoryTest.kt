package com.zeroos.core.data.docs

import androidx.datastore.core.DataStoreFactory
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.data.settings.SettingsSerializer
import com.zeroos.core.logging.LogLevel
import com.zeroos.core.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DocumentsRepositoryTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val logged = mutableListOf<String>()
    private val logger = object : Logger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            logged += message
        }
    }
    private val texts = mutableMapOf(
        ReleaseDocument.README to "readme",
        ReleaseDocument.LICENSE to "license",
        ReleaseDocument.NOTICES to "notices"
    )

    private fun TestScope.repository(): Pair<DocumentsRepository, SettingsRepository> {
        val store = DataStoreFactory.create(SettingsSerializer, scope = backgroundScope, produceFile = {
            temp.newFile().apply { delete() }
        })
        val settings = SettingsRepository(store, logger)
        return DocumentsRepository({ texts[it] }, settings, Dispatchers.Unconfined, logger, "0.0.30") to settings
    }

    @Test
    fun f14_8_firstStartAsksThenAcceptingRemembersTheseDocuments() = runTest {
        val (docs, settings) = repository()
        assertEquals(DocumentsGate.LOADING, docs.gate.first())
        docs.load()
        assertEquals(DocumentsGate.FIRST, docs.gate.first())
        docs.accept()
        assertEquals(DocumentsGate.ACCEPTED, docs.gate.first())
        assertEquals(fingerprintOf(texts.values.toList()), settings.settings.first().acceptedDocuments)
        assertTrue(logged.contains("documents accepted for 0.0.30"))
    }

    @Test
    fun f14_8_aReleaseThatChangesTheDocumentsAsksOnceMore() = runTest {
        val (docs, settings) = repository()
        settings.update("test") { it.copy(acceptedDocuments = "an older fingerprint") }
        docs.load()
        assertEquals(DocumentsGate.CHANGED, docs.gate.first())
    }

    @Test
    fun f14_8_missingDocumentsNeverLockThePhone() = runTest {
        texts.remove(ReleaseDocument.NOTICES)
        val (docs, _) = repository()
        docs.load()
        assertEquals(DocumentsGate.ACCEPTED, docs.gate.first())
        assertTrue(logged.contains("release documents missing; not asking to accept them"))
        assertEquals("", docs.text(ReleaseDocument.NOTICES))
    }

    @Test
    fun f14_8_fingerprintChangesWithAnyDocument() {
        val first = fingerprintOf(listOf("a", "b", "c"))
        assertNotEquals(first, fingerprintOf(listOf("a", "b", "c!")))
        assertNotEquals("splitting differently changes it", fingerprintOf(listOf("ab", "", "c")), first)
        assertNull(fingerprintOf(listOf("a", null, "c")))
    }
}
