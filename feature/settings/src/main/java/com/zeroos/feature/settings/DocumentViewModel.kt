package com.zeroos.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zeroos.core.data.docs.DocumentsRepository
import com.zeroos.core.data.docs.ReleaseDocument
import com.zeroos.core.resources.R
import com.zeroos.core.ui.DocumentReader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Reads one release document for Settings → About (F14.8, wireframe K3). */
@HiltViewModel
class DocumentViewModel @Inject constructor(private val documents: DocumentsRepository) : ViewModel() {
    private val mutableText = MutableStateFlow("")
    val text: StateFlow<String> = mutableText.asStateFlow()

    fun load(document: ReleaseDocument) {
        viewModelScope.launch { mutableText.value = documents.text(document) }
    }
}

/** The settings page that shows [page]'s document; only for the three document pages. */
internal fun SettingsPage.document(): ReleaseDocument? = when (this) {
    SettingsPage.DOC_README -> ReleaseDocument.README
    SettingsPage.DOC_LICENSE -> ReleaseDocument.LICENSE
    SettingsPage.DOC_NOTICES -> ReleaseDocument.NOTICES
    else -> null
}

internal fun ReleaseDocument.page(): SettingsPage = when (this) {
    ReleaseDocument.README -> SettingsPage.DOC_README
    ReleaseDocument.LICENSE -> SettingsPage.DOC_LICENSE
    ReleaseDocument.NOTICES -> SettingsPage.DOC_NOTICES
}

internal fun ReleaseDocument.title(): Int = when (this) {
    ReleaseDocument.README -> R.string.docs_readme
    ReleaseDocument.LICENSE -> R.string.docs_license
    ReleaseDocument.NOTICES -> R.string.docs_notices
}

@Composable
fun DocumentRoute(document: ReleaseDocument, onBack: () -> Unit, viewModel: DocumentViewModel = hiltViewModel(key = document.name)) {
    LaunchedEffect(document) { viewModel.load(document) }
    val text by viewModel.text.collectAsStateWithLifecycle()
    DocumentReader(stringResource(document.title()), text, onBack)
}
