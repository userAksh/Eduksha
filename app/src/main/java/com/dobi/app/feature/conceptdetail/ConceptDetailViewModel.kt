package com.dobi.app.feature.conceptdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.model.Completion
import com.dobi.app.domain.model.Difficulty
import com.dobi.app.domain.model.WorkMode
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptByIdUseCase
import com.dobi.app.domain.usecase.SaveCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConceptDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getConceptByIdUseCase: GetConceptByIdUseCase,
    private val saveCompletionUseCase: SaveCompletionUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val conceptId: String = savedStateHandle.toRoute<Screen.ConceptDetail>().conceptId

    private val _uiState = MutableStateFlow(ConceptDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadConcept()
        observeUploadResult()
    }

    private fun observeUploadResult() {
        savedStateHandle.getStateFlow<String?>(KEY_UPLOAD_RESULT, null)
            .onEach { kindKey ->
                val mode = when (kindKey) {
                    "class_work" -> WorkMode.CLASS_WORK
                    "self_learning" -> WorkMode.SELF_LEARNING
                    else -> null
                }
                if (mode != null) {
                    _uiState.update { it.copy(selectedWorkMode = mode) }
                    savedStateHandle[KEY_UPLOAD_RESULT] = null
                }
            }
            .launchIn(viewModelScope)
    }

    companion object {
        const val KEY_UPLOAD_RESULT = "uploaded_kind"
    }

    private fun loadConcept() {
        viewModelScope.launch {
            getConceptByIdUseCase(conceptId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { concept ->
                    _uiState.update { it.copy(concept = concept, isLoading = false) }
                }
        }
    }

    fun onWorkModeSelected(mode: WorkMode) = _uiState.update { it.copy(selectedWorkMode = mode) }

    fun onDifficultySelected(difficulty: Difficulty) = _uiState.update { it.copy(selectedDifficulty = difficulty) }

    fun onMarkAsDone() {
        val state = _uiState.value
        val mode = state.selectedWorkMode ?: return
        val difficulty = state.selectedDifficulty ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val childId = userRepository.linkedChildId.first().orEmpty()
            if (childId.isBlank()) {
                _uiState.update { it.copy(isSaving = false, error = "Couldn't find your profile.") }
                return@launch
            }
            runCatching {
                saveCompletionUseCase(
                    Completion(
                        childId = childId,
                        conceptId = conceptId,
                        effort = difficulty,
                        mode = mode
                    )
                )
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
