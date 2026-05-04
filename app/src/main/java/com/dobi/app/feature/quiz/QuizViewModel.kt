package com.dobi.app.feature.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.AttemptScope
import com.dobi.app.data.repository.CuratedQuizCache
import com.dobi.app.data.repository.GeneratedQuizCache
import com.dobi.app.data.repository.McqGenerationRepository
import com.dobi.app.data.repository.QuizAttemptRepository
import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.repository.ParentTaskRepository
import com.dobi.app.domain.repository.StreakShareRepository
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
    private val curatedQuizCache: CuratedQuizCache,
    private val mcqGenerationRepository: McqGenerationRepository,
    private val userRepository: UserRepository,
    private val streakShareRepository: StreakShareRepository,
    private val parentTaskRepository: ParentTaskRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.Quiz>()
    private val isConceptScope: Boolean = route.conceptId.isNotBlank()
    private val targetId: String = if (isConceptScope) route.conceptId else route.unitId
    private val attemptScope: AttemptScope =
        if (isConceptScope) AttemptScope.CONCEPT else AttemptScope.UNIT
    private val viewResult: Boolean = route.viewResult
    private val isAiSource: Boolean = route.source == "ai"
    private val routeDisplayName: String = route.displayName
    private val parentTaskId: String = route.parentTaskId

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
        observeShareGate()
    }

    private fun observeShareGate() {
        viewModelScope.launch {
            streakShareRepository.hasSharedToday().collect { hasShared ->
                _uiState.update { it.copy(showSnapAndSend = !hasShared) }
            }
        }
    }

    /** Called from the screen after the snap share intent has been fired. */
    fun markSnapSent() {
        viewModelScope.launch { streakShareRepository.markSharedToday() }
    }

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

                    var questions = aiQuestions ?: if (isConceptScope) {
                        getCuratedQuestionsUseCase.forConcept(targetId)
                    } else {
                        getCuratedQuestionsUseCase(targetId)
                    }

                    // Fallback for curated path: when the hand-written
                    // CuratedQuestionBank doesn't cover this lesson/concept
                    // we generate 10 MCQs from the syllabus content via
                    // Gemini and cache them so retakes don't regenerate.
                    if (!isAiSource && questions.isEmpty()) {
                        val cached = curatedQuizCache.get(targetId)
                        if (cached != null) {
                            questions = cached
                        } else {
                            _uiState.update {
                                it.copy(isLoading = false, isGeneratingCurated = true)
                            }
                            val grade = userRepository.linkedChildGrade.first()
                            val generated = generateCuratedFallback(
                                concepts = concepts,
                                grade = grade
                            )
                            if (generated.isFailure) {
                                _uiState.update {
                                    it.copy(
                                        isGeneratingCurated = false,
                                        error = friendlyGenerationError(
                                            generated.exceptionOrNull()?.message
                                        )
                                    )
                                }
                                return@collect
                            }
                            questions = generated.getOrNull().orEmpty()
                            curatedQuizCache.put(targetId, questions)
                        }
                    }

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
                            isLoading = false,
                            isGeneratingCurated = false
                        )
                    }
                }
        }
    }

    private suspend fun generateCuratedFallback(
        concepts: List<Concept>,
        grade: Int
    ): Result<List<com.dobi.app.domain.model.McqQuestion>> {
        val (subject, unitName, conceptName, conceptDescription) = if (isConceptScope) {
            val concept = concepts.firstOrNull { it.id == targetId }
            QuadStrings(
                subject = concept?.subject.orEmpty(),
                unitName = concept?.unit.orEmpty(),
                conceptName = concept?.name.orEmpty(),
                description = concept?.description.orEmpty()
            )
        } else {
            val unitConcepts = concepts.filter { it.unitId == targetId }
            val first = unitConcepts.firstOrNull()
            // For unit-scope, stitch together every concept's name + description
            // so Gemini can generate broadly across the lesson, not just one
            // concept inside it.
            QuadStrings(
                subject = first?.subject.orEmpty(),
                unitName = first?.unit.orEmpty(),
                conceptName = "",
                description = unitConcepts.joinToString("\n\n") { c ->
                    "• ${c.name}: ${c.description}"
                }
            )
        }
        return mcqGenerationRepository.generateFromLesson(
            subject = subject,
            grade = grade,
            unitName = unitName,
            conceptName = conceptName,
            conceptDescription = conceptDescription,
            targetCount = 10
        )
    }

    private data class QuadStrings(
        val subject: String,
        val unitName: String,
        val conceptName: String,
        val description: String
    )

    private fun friendlyGenerationError(raw: String?): String {
        val msg = raw.orEmpty().trim().lowercase()
        return when {
            "unavailable" in msg || "overloaded" in msg || "503" in msg ->
                "Dobi's brain is a little busy right now. Please try again in a minute."
            "quota" in msg || "rate" in msg && "limit" in msg ->
                "We've hit today's quiz limit. Try again later."
            "network" in msg || "timeout" in msg || "unable to" in msg ->
                "No internet right now. Check your connection and try again."
            else -> "Couldn't prepare the quiz. Please try again."
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
            val correctCount = current.revealed.count { idx ->
                current.selections[idx] == current.questions.getOrNull(idx)?.correctIndex
            }
            val total = current.questions.size
            if (!isAiSource) {
                viewModelScope.launch {
                    quizAttemptRepository.recordAttempt(
                        scope = attemptScope,
                        targetId = targetId,
                        selections = current.selections,
                        correctCount = correctCount,
                        totalQuestions = total,
                        displayName = current.unitName,
                        subject = current.subject
                    )
                }
            }
            if (parentTaskId.isNotBlank()) {
                viewModelScope.launch {
                    parentTaskRepository.submitByChild(
                        taskId = parentTaskId,
                        photoUrls = emptyList(),
                        score = correctCount,
                        total = total
                    )
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
