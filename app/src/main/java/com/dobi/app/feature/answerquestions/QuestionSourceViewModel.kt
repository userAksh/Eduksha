package com.dobi.app.feature.answerquestions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.AiPracticeContext
import com.dobi.app.data.repository.AiPracticeContextHolder
import com.dobi.app.data.repository.AttemptScope
import com.dobi.app.data.repository.PreselectedWriteMode
import com.dobi.app.data.repository.QuizAttemptRepository
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QuestionSourceNavEvent {
    data object LaunchWriteAnswers : QuestionSourceNavEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuestionSourceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getConceptsUseCase: GetConceptsUseCase,
    private val quizAttemptRepository: QuizAttemptRepository,
    private val userRepository: UserRepository,
    private val aiPracticeContextHolder: AiPracticeContextHolder,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.AnswerQuestionsSource>()
    private val isConceptScope: Boolean = route.conceptId.isNotBlank()
    private val targetId: String = if (isConceptScope) route.conceptId else route.unitId
    private val attemptScope: AttemptScope =
        if (isConceptScope) AttemptScope.CONCEPT else AttemptScope.UNIT

    private val _uiState = MutableStateFlow(QuestionSourceUiState())
    val uiState = _uiState.asStateFlow()

    private val _navEvents = Channel<QuestionSourceNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    // Cached for Write Answers context preparation
    private var cachedGrade: Int = 6
    private var cachedUnitName: String = ""  // parent unit name (when concept scope)

    init {
        loadContext()
        observeAttempts()
    }

    private fun observeAttempts() {
        quizAttemptRepository.attempts(attemptScope, targetId)
            .onEach { count -> _uiState.update { it.copy(attemptsTaken = count) } }
            .launchIn(viewModelScope)
    }

    private fun loadContext() {
        viewModelScope.launch {
            userRepository.linkedChildGrade
                .flatMapLatest { grade ->
                    cachedGrade = grade
                    getConceptsUseCase(grade = grade, board = "CBSE")
                }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { concepts ->
                    val (lessonName, subject) = if (isConceptScope) {
                        val concept = concepts.firstOrNull { it.id == targetId }
                        cachedUnitName = concept?.unit.orEmpty()
                        (concept?.name.orEmpty()) to (concept?.subject.orEmpty())
                    } else {
                        val first = concepts.firstOrNull { it.unitId == targetId }
                        cachedUnitName = first?.unit.orEmpty()
                        (first?.unit.orEmpty()) to (first?.subject.orEmpty())
                    }
                    _uiState.update {
                        it.copy(
                            unitName = lessonName,
                            subject = subject,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onWriteAnswersClick() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isLoading || state.unitName.isBlank()) return@launch
            val scopeText = if (isConceptScope) {
                "Lesson: $cachedUnitName\nConcept (focus narrowly on this): ${state.unitName}"
            } else {
                "Lesson: ${state.unitName}"
            }
            val key = if (isConceptScope) "qs_concept_${route.conceptId}"
                      else "qs_unit_${route.unitId}"
            aiPracticeContextHolder.put(
                AiPracticeContext(
                    selectionKey = key,
                    displayName = state.unitName,
                    subject = state.subject,
                    grade = cachedGrade,
                    scopeText = scopeText,
                    preselectedWriteMode = PreselectedWriteMode.GENERATED,
                    unitId = route.unitId,
                    unitName = cachedUnitName,
                    conceptId = route.conceptId,
                    conceptName = if (isConceptScope) state.unitName else ""
                )
            )
            _navEvents.send(QuestionSourceNavEvent.LaunchWriteAnswers)
        }
    }
}
