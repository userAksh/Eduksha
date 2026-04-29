package com.dobi.app.feature.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.AttemptScope
import com.dobi.app.data.repository.GeneratedQuizCache
import com.dobi.app.data.repository.QuizAttemptRepository
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import com.dobi.app.domain.usecase.GetCuratedQuestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getConceptsUseCase: GetConceptsUseCase,
    private val getCuratedQuestionsUseCase: GetCuratedQuestionsUseCase,
    private val quizAttemptRepository: QuizAttemptRepository,
    private val generatedQuizCache: GeneratedQuizCache,
    private val userRepository: UserRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.Quiz>()
    private val isConceptScope: Boolean = route.conceptId.isNotBlank()
    private val targetId: String = if (isConceptScope) route.conceptId else route.unitId
    private val attemptScope: AttemptScope =
        if (isConceptScope) AttemptScope.CONCEPT else AttemptScope.UNIT
    private val viewResult: Boolean = route.viewResult
    private val isAiSource: Boolean = route.source == "ai"
    private val routeDisplayName: String = route.displayName

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val attempts = if (isAiSource) 0
                else quizAttemptRepository.attempts(attemptScope, targetId).first()
            val savedSelections = if (viewResult && !isAiSource) {
                quizAttemptRepository.lastSelections(attemptScope, targetId).first()
            } else emptyMap()

            val aiQuestions = if (isAiSource) generatedQuizCache.consume(targetId) else null
            if (isAiSource && aiQuestions.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Your generated quiz wasn't found. Please upload again."
                    )
                }
                return@launch
            }

            userRepository.linkedChildGrade
                .flatMapLatest { grade -> getConceptsUseCase(grade = grade, board = "CBSE") }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { concepts ->
                    val (lessonName, subject) = if (isConceptScope) {
                        val concept = concepts.firstOrNull { it.id == targetId }
                        (concept?.name.orEmpty()) to (concept?.subject.orEmpty())
                    } else {
                        val first = concepts.firstOrNull { it.unitId == targetId }
                        (first?.unit.orEmpty()) to (first?.subject.orEmpty())
                    }
                    val effectiveLessonName =
                        if (routeDisplayName.isNotBlank()) routeDisplayName else lessonName
                    val questions = aiQuestions
                        ?: if (isConceptScope) getCuratedQuestionsUseCase.forConcept(targetId)
                           else getCuratedQuestionsUseCase(targetId)
                    _uiState.update {
                        it.copy(
                            unitName = effectiveLessonName,
                            subject = subject,
                            questions = questions,
                            attemptsTaken = attempts,
                            selections = savedSelections,
                            revealed = if (viewResult) savedSelections.keys else emptySet(),
                            phase = if (viewResult && savedSelections.isNotEmpty()) {
                                QuizPhase.DONE
                            } else QuizPhase.INTRO,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun startQuiz() = _uiState.update {
        it.copy(phase = QuizPhase.QUESTION, currentIndex = 0)
    }

    fun selectOption(optionIndex: Int) = _uiState.update { state ->
        if (state.isCurrentRevealed) return@update state
        state.copy(
            selections = state.selections + (state.currentIndex to optionIndex),
            revealed = state.revealed + state.currentIndex
        )
    }

    fun next() {
        val current = _uiState.value
        if (!current.isCurrentRevealed) return

        if (current.isLastQuestion) {
            _uiState.update { it.copy(
                phase = QuizPhase.DONE,
                attemptsTaken = it.attemptsTaken + 1
            ) }
            if (!isAiSource) {
                viewModelScope.launch {
                    quizAttemptRepository.recordAttempt(attemptScope, targetId, current.selections)
                }
            }
        } else {
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
        }
    }

    fun previous() = _uiState.update { state ->
        if (state.canGoPrevious) state.copy(currentIndex = state.currentIndex - 1) else state
    }

    fun retry() = _uiState.update { state ->
        if (!state.canRetry) state
        else state.copy(
            phase = QuizPhase.INTRO,
            currentIndex = 0,
            selections = emptyMap(),
            revealed = emptySet()
        )
    }
}
