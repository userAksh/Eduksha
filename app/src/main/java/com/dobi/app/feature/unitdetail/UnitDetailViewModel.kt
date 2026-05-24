package com.dobi.app.feature.unitdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.AttemptScope
import com.dobi.app.data.repository.QuizAttemptRepository
import com.dobi.app.data.repository.RevisionMarkRepository
import com.dobi.app.data.repository.UploadEntryRepository
import com.dobi.app.domain.model.Completion
import com.dobi.app.domain.model.Difficulty
import com.dobi.app.domain.model.RevisionMark
import com.dobi.app.domain.model.RevisionScope
import com.dobi.app.domain.model.WorkMode
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import com.dobi.app.domain.usecase.SaveCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    private val userRepository: UserRepository,
    private val revisionMarkRepository: RevisionMarkRepository,
    private val uploadEntryRepository: UploadEntryRepository,
    private val quizAttemptRepository: QuizAttemptRepository
) : ViewModel() {

    private val unitId: String = savedStateHandle.toRoute<Screen.UnitDetail>().unitId

    private val _uiState = MutableStateFlow(UnitDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUnit()
        observeUploadResult()
        observeRevisionMark()
        observeActivity()
    }

    private fun observeActivity() {
        combine(
            uploadEntryRepository.observeForActiveChild(),
            quizAttemptRepository.attempts(AttemptScope.UNIT, unitId)
        ) { entries, quizCount ->
            val hasUpload = entries.any {
                it.unitId == unitId &&
                    (it.kind == "class_work" || it.kind == "self_learning")
            }
            hasUpload || quizCount > 0
        }
            .catch { }
            .onEach { hasActivity -> _uiState.update { it.copy(hasActivity = hasActivity) } }
            .launchIn(viewModelScope)
    }

    private fun observeRevisionMark() {
        revisionMarkRepository.isMarked(RevisionScope.UNIT, unitId)
            .onEach { marked -> _uiState.update { it.copy(isMarkedForRevision = marked) } }
            .launchIn(viewModelScope)
    }

    fun onToggleRevisionMark() {
        if (_uiState.value.isTogglingRevision) return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isTogglingRevision = true) }
            val mark = RevisionMark(
                scope = RevisionScope.UNIT.key,
                targetId = unitId,
                name = state.unitName,
                subject = state.subject,
                subjectColor = state.subjectColor,
                subjectIcon = state.subjectIcon,
                unitId = unitId,
                unitName = state.unitName
            )
            revisionMarkRepository.toggle(mark)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Couldn't save. Try again.") }
                }
            _uiState.update { it.copy(isTogglingRevision = false) }
        }
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

    fun onToastShown() = _uiState.update { it.copy(toastMessage = null) }

    fun onMarkAsDone() {
        val state = _uiState.value
        val mode = state.selectedWorkMode ?: return
        val difficulty = state.selectedDifficulty ?: return
        val coveredConcepts = state.concepts.filter { it.id in state.coveredConceptIds }
        if (coveredConcepts.isEmpty()) return

        if (!state.hasActivity) {
            _uiState.update {
                it.copy(toastMessage = "Please upload your work or answer questions first")
            }
            return
        }

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
