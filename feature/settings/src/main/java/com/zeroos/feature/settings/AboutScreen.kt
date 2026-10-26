package com.zeroos.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.docs.ReleaseDocument
import com.zeroos.core.logging.Logger
import com.zeroos.core.logging.warn
import com.zeroos.core.resources.R
import com.zeroos.core.ui.BodyText
import com.zeroos.core.ui.ZeroTheme
import com.zeroos.core.ui.ZeroWordmark
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AboutState(val versionName: String = "", val versionCode: Long = 0, val dotoLicense: String = "")

/** Version and open-source licenses (wireframe F7). Reading the package and the license asset happens off the main thread. */
@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
    ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val mutableState = MutableStateFlow(AboutState())
    val state: StateFlow<AboutState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.value = withContext(ioDispatcher) { load() }
        }
    }

    private fun load(): AboutState {
        val (name, code) = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            (info.versionName.orEmpty()) to PackageInfoCompat.getLongVersionCode(info)
        } catch (e: PackageManager.NameNotFoundException) {
            logger.warn(TAG, "own package info missing", e)
            "" to 0L
        }
        val license = try {
            context.assets.open(DOTO_LICENSE).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            logger.warn(TAG, "license asset missing", e)
            ""
        }
        return AboutState(name, code, license)
    }

    private companion object {
        const val TAG = "about"
        const val DOTO_LICENSE = "licenses/doto-OFL.txt"
    }
}

@Composable
fun AboutRoute(
    onLicenses: () -> Unit,
    onDocument: (ReleaseDocument) -> Unit,
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutScreen(state, onLicenses, onBack, onDocument)
}

/** Wireframe F7, with the release's documents (F14.8, K3). */
@Composable
fun AboutScreen(state: AboutState, onLicenses: () -> Unit, onBack: () -> Unit, onDocument: (ReleaseDocument) -> Unit = {}) {
    SettingsPageScaffold(stringResource(R.string.settings_about), SettingsTestTags.ABOUT, onBack) {
        item {
            ZeroWordmark(
                Modifier.padding(horizontal = ZeroTheme.tokens.spacing.screenGutter, vertical = ZeroTheme.tokens.spacing.sectionGap)
            )
        }
        item {
            BodyText(stringResource(R.string.about_version, stringResource(R.string.app_name), state.versionName, state.versionCode))
        }
        items(ReleaseDocument.entries) { document -> NavRow(document.title()) { onDocument(document) } }
        item { NavRow(R.string.about_licenses, onLicenses) }
    }
}

@Composable
fun LicensesRoute(onBack: () -> Unit, viewModel: AboutViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LicensesScreen(state.dotoLicense, onBack)
}

/** The Doto font license in full (OFL) and the libraries used. */
@Composable
fun LicensesScreen(dotoLicense: String, onBack: () -> Unit) {
    SettingsPageScaffold(stringResource(R.string.about_licenses), SettingsTestTags.LICENSES, onBack) {
        item { BodyText(stringResource(R.string.licenses_libraries)) }
        item { BodyText(stringResource(R.string.licenses_doto)) }
        item { BodyText(dotoLicense, muted = true, small = true) }
    }
}
