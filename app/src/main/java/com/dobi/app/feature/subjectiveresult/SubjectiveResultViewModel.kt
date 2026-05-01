package com.dobi.app.feature.subjectiveresult

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.SubjectiveResultRepository
import com.dobi.app.domain.model.SubjectiveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectiveResultUiState(
    val result: SubjectiveResult? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SubjectiveResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subjectiveResultRepository: SubjectiveResultRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.SubjectiveResult>()

    private val _uiState = MutableStateFlow(SubjectiveResultUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            subjectiveResultRepository.getById(route.resultId)
                .onSuccess { res ->
                    _uiState.update { it.copy(result = res, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load result") }
                }
        }
    }
}
