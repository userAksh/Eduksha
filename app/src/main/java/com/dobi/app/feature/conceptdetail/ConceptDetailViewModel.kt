package com.dobi.app.feature.conceptdetail

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
import com.dobi.app.domain.usecase.GetConceptByIdUseCase
import com.dobi.app.domain.usecase.SaveCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    private val userRepository: UserRepository,
    private val revisionMarkRepository: RevisionMarkRepository,
    private val uploadEntryRepository: UploadEntryRepository,
    private val quizAttemptRepository: QuizAttemptRepository
) : ViewModel() {

    private val conceptId: String = savedStateHandle.toRoute<Screen.ConceptDetail>().conceptId

    private val _uiState = MutableStateFlow(ConceptDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadConcept()
        observeUploadResult()
        observeRevisionMark()
        observeActivity()
    }

    private fun observeActivity() {
        combine(
            uploadEntryRepository.observeForActiveChild(),
            quizAttemptRepository.attempts(AttemptScope.CONCEPT, conceptId)
        ) { entries, quizCount ->
            val classWork = entries.any { it.conceptId == conceptId && it.kind == "class_work" }
            val selfStudy = entries.any { it.conceptId == conceptId && it.kind == "self_learning" }
            val hasQuiz = quizCount > 0
            val hasActivity = classWork || selfStudy || hasQuiz
            val detectedMode = when {
                classWork -> WorkMode.CLASS_WORK
                selfStudy -> WorkMode.SELF_LEARNING
                hasQuiz   -> WorkMode.QUESTIONS
                else      -> null
            }
            hasActivity to detectedMode
        }
            .catch { }
            .onEach { (hasActivity, detectedMode) ->
                _uiState.update { it.copy(hasActivity = hasActivity, detectedWorkMode = detectedMode) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeRevisionMark() {
        revisionMarkRepository.isMarked(RevisionScope.CONCEPT, conceptId)
            .onEach { marked -> _uiState.update { it.copy(isMarkedForRevision = marked) } }
            .launchIn(viewModelScope)
    }

    fun onToggleRevisionMark() {
        val concept = _uiState.value.concept ?: return
        if (_uiState.value.isTogglingRevision) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTogglingRevision = true) }
            val mark = RevisionMark(
                scope = RevisionScope.CONCEPT.key,
                targetId = conceptId,
                name = concept.name,
                subject = concept.subject,
                subjectColor = concept.subjectColor,
                subjectIcon = concept.subjectIcon,
                unitId = concept.unitId,
                unitName = concept.unit
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

    fun onToastShown() = _uiState.update { it.copy(toastMessage = null) }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    fun onMarkAsDone() {
        val state = _uiState.value
        val difficulty = state.selectedDifficulty ?: return

        if (!state.hasActivity) {
            _uiState.update {
                it.copy(toastMessage = "First attempt a quiz, upload class work, or upload self learning to mark this as done.")
            }
            return
        }

        val mode = state.selectedWorkMode ?: state.detectedWorkMode ?: WorkMode.QUESTIONS

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
