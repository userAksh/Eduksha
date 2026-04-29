package com.dobi.app.feature.unitdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.model.Completion
import com.dobi.app.domain.model.Difficulty
import com.dobi.app.domain.model.WorkMode
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import com.dobi.app.domain.usecase.SaveCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UnitDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getConceptsUseCase: GetConceptsUseCase,
    private val saveCompletionUseCase: SaveCompletionUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val unitId: String = savedStateHandle.toRoute<Screen.UnitDetail>().unitId

    private val _uiState = MutableStateFlow(UnitDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUnit()
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

    private fun loadUnit() {
        viewModelScope.launch {
            userRepository.linkedChildGrade
                .flatMapLatest { grade -> getConceptsUseCase(grade = grade, board = "CBSE") }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { allConcepts ->
                    val unitConcepts = allConcepts
                        .filter { it.unitId == unitId }
                        .sortedBy { it.order }
                    if (unitConcepts.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, error = "Lesson not found") }
                    } else {
                        val first = unitConcepts.first()
                        _uiState.update {
                            it.copy(
                                unitName = first.unit,
                                subject = first.subject,
                                subjectColor = first.subjectColor,
                                subjectIcon = first.subjectIcon,
                                concepts = unitConcepts,
                                coveredConceptIds = emptySet(),
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                }
        }
    }

    fun onWorkModeSelected(mode: WorkMode) = _uiState.update { it.copy(selectedWorkMode = mode) }

    fun onDifficultySelected(difficulty: Difficulty) = _uiState.update { it.copy(selectedDifficulty = difficulty) }

    fun onConceptCoverageToggled(conceptId: String) = _uiState.update { state ->
        val next = state.coveredConceptIds.toMutableSet().apply {
            if (!add(conceptId)) remove(conceptId)
        }
        state.copy(coveredConceptIds = next)
    }

    fun onMarkAsDone() {
        val state = _uiState.value
        val mode = state.selectedWorkMode ?: return
        val difficulty = state.selectedDifficulty ?: return
        val coveredConcepts = state.concepts.filter { it.id in state.coveredConceptIds }
        if (coveredConcepts.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val childId = userRepository.linkedChildId.first().orEmpty()
            if (childId.isBlank()) {
                _uiState.update { it.copy(isSaving = false, error = "Couldn't find your profile.") }
                return@launch
            }
            runCatching {
                coveredConcepts.forEach { concept ->
                    saveCompletionUseCase(
                        Completion(
                            childId = childId,
                            conceptId = concept.id,
                            effort = difficulty,
                            mode = mode
                        )
                    )
                }
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    companion object {
        const val KEY_UPLOAD_RESULT = "uploaded_kind"
    }
}
