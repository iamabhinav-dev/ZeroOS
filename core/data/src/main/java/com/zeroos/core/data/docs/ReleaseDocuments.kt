package com.zeroos.core.data.docs

import android.content.res.AssetManager
import com.zeroos.core.data.settings.SettingsRepository
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.info
import com.zeroos.core.logging.warn
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** The documents every user reads and accepts before using ZeroOS (F14.8), copied into the APK at build time. */
enum class ReleaseDocument(val asset: String) {
    README("docs/README.md"),
    LICENSE("docs/LICENSE.txt"),
    NOTICES("docs/THIRD_PARTY_NOTICES.md")
}

/** Reads a document's text, or null if it's missing; the app's assets, or a fake in tests. */
fun interface DocumentSource {
    fun read(document: ReleaseDocument): String?
}

class AssetDocumentSource(private val assets: AssetManager) : DocumentSource {
    override fun read(document: ReleaseDocument): String? = try {
        assets.open(document.asset).bufferedReader().use { it.readText() }
    } catch (@Suppress("SwallowedException") e: IOException) {
        null
    }
}

/** Whether the documents page has to come before anything else (F14.8, wireframe K2). */
enum class DocumentsGate {
    /** Still reading the documents or settings: show nothing yet. */
    LOADING,

    /** Never accepted on this phone: "Before you start". */
    FIRST,

    /** Accepted once, but this release changed them: "The documents changed". */
    CHANGED,

    /** Accepted as they are, or they couldn't be read (never lock the user out of their phone). */
    ACCEPTED
}

/**
 * The release's documents and whether they've been accepted (F14.8). Acceptance is stored as a fingerprint of
 * all three texts, so a release that doesn't change them doesn't ask again.
 */
class DocumentsRepository(
    private val source: DocumentSource,
    private val settings: SettingsRepository,
    private val io: CoroutineDispatcher,
    private val logger: Logger,
    /** The installed version's name, shown on the documents page. */
    val versionName: String
) {
    private val fingerprint = MutableStateFlow<Fingerprint?>(null)

    private class Fingerprint(val value: String?)

    val gate: Flow<DocumentsGate> = combine(fingerprint, settings.settings) { current, s ->
        val id = current?.value
        when {
            current == null -> DocumentsGate.LOADING
            id == null || s.acceptedDocuments == id -> DocumentsGate.ACCEPTED
            s.acceptedDocuments == null -> DocumentsGate.FIRST
            else -> DocumentsGate.CHANGED
        }
    }

    /** Reads the documents once, off the main thread, to know whether they changed. */
    suspend fun load() {
        if (fingerprint.value != null) return
        val id = withContext(io) { fingerprintOf(ReleaseDocument.entries.map { source.read(it) }) }
        if (id == null) logger.warn(TAG, "release documents missing; not asking to accept them")
        fingerprint.value = Fingerprint(id)
    }

    suspend fun text(document: ReleaseDocument): String = withContext(io) { source.read(document).orEmpty() }

    /** The user ticked the box and tapped Continue (F14.8). */
    suspend fun accept() {
        val id = fingerprint.filterNotNull().first().value ?: return
        settings.update("documents accepted (${id.take(SHORT_ID)})") { it.copy(acceptedDocuments = id) }
        logger.info(TAG, "documents accepted for $versionName")
    }

    private companion object {
        const val TAG = "documents"
        const val SHORT_ID = 8
    }
}

/** SHA-256 of the documents' texts, or null if any is missing. */
fun fingerprintOf(texts: List<String?>): String? {
    if (texts.any { it == null }) return null
    val digest = MessageDigest.getInstance("SHA-256")
    texts.forEach {
        digest.update(requireNotNull(it).toByteArray())
        digest.update(0.toByte())
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
