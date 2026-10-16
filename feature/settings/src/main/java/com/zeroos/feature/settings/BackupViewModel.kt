package com.zeroos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.backup.BackupReadResult
import com.zeroos.core.data.backup.BackupRepository
import com.zeroos.core.data.backup.SettingsBackup
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.warn
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Result line shown under the backup rows (wireframe F6). */
sealed interface BackupMessage {
    data object Exported : BackupMessage

    data object Imported : BackupMessage

    data object NotABackup : BackupMessage

    data class TooNew(val version: Int) : BackupMessage

    data object Failed : BackupMessage
}

data class BackupState(
    val lastExportMillis: Long? = null,
    val message: BackupMessage? = null,
    /** A read backup waiting for "Replace all settings?" (F13.2). */
    val pending: SettingsBackup? = null
)

/** Export/import of all settings (F13). Streams come from the system file picker. */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backup: BackupRepository,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val mutableState = MutableStateFlow(BackupState(lastExportMillis = backup.lastExportMillis))
    val state: StateFlow<BackupState> = mutableState.asStateFlow()

    fun suggestedFileName(): String = backup.suggestedFileName()

    fun export(open: () -> OutputStream?) = runIo("export") {
        val stream = open() ?: throw IOException("no output stream")
        stream.use { backup.export(it) }
        mutableState.update { it.copy(lastExportMillis = backup.lastExportMillis, message = BackupMessage.Exported) }
    }

    fun read(open: () -> InputStream?) = runIo("import") {
        val stream = open() ?: throw IOException("no input stream")
        when (val result = stream.use { backup.read(it) }) {
            is BackupReadResult.Ok -> mutableState.update { it.copy(pending = result.backup, message = null) }
            BackupReadResult.NotABackup -> mutableState.update { it.copy(message = BackupMessage.NotABackup) }
            is BackupReadResult.TooNew -> mutableState.update { it.copy(message = BackupMessage.TooNew(result.version)) }
        }
    }

    fun confirmImport() {
        val pending = state.value.pending ?: return
        runIo("import") {
            backup.restore(pending)
            mutableState.update { it.copy(pending = null, message = BackupMessage.Imported) }
        }
    }

    fun cancelImport() = mutableState.update { it.copy(pending = null) }

    private fun runIo(what: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) { block() }
            } catch (e: IOException) {
                logger.warn(TAG, "$what failed", e)
                mutableState.update { it.copy(message = BackupMessage.Failed, pending = null) }
            } catch (e: SecurityException) {
                logger.warn(TAG, "$what failed", e)
                mutableState.update { it.copy(message = BackupMessage.Failed, pending = null) }
            }
        }
    }

    private companion object {
        const val TAG = "backup"
    }
}
