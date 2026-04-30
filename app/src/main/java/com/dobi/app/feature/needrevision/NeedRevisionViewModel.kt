package com.dobi.app.feature.needrevision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.RevisionMarkRepository
import com.dobi.app.domain.model.RevisionMark
import com.dobi.app.domain.model.RevisionScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NeedRevisionUiState(
    val items: List<RevisionMark> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NeedRevisionViewModel @Inject constructor(
    private val revisionMarkRepository: RevisionMarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NeedRevisionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            revisionMarkRepository.observeForActiveChild()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { items -> _uiState.update { it.copy(items = items, isLoading = false) } }
        }
    }

    fun onUnmark(mark: RevisionMark) {
        viewModelScope.launch {
            revisionMarkRepository.toggle(
                mark.copy(scope = RevisionScope.fromKey(mark.scope).key)
            )
        }
    }
}
