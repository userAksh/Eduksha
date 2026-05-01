package com.dobi.app.feature.testyourself

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.AiPracticeContext
import com.dobi.app.data.repository.AiPracticeContextHolder
import com.dobi.app.data.repository.CuratedQuizCache
import com.dobi.app.data.repository.McqGenerationRepository
import com.dobi.app.data.repository.PreselectedWriteMode
import com.dobi.app.domain.model.Concept
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

data class TestYourselfFormatUiState(
    val source: String = SOURCE_CURATED,
    val isUnitScope: Boolean = false,
    val unitId: String = "",
    val conceptId: String = "",
    val title: String = "",
    val subject: String = "",
    val subjectColor: String = "",
    val subjectIcon: String = "",
    val unitName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val isAi: Boolean get() = source == SOURCE_AI
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TestYourselfFormatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val getConceptsUseCase: GetConceptsUseCase,
    private val getCuratedQuestionsUseCase: GetCuratedQuestionsUseCase,
    private val curatedQuizCache: CuratedQuizCache,
    private val mcqGenerationRepository: McqGenerationRepository,
    private val aiPracticeContextHolder: AiPracticeContextHolder
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.TestYourselfFormat>()
    private val isUnitScope: Boolean = route.conceptId.isBlank()

    private val _uiState = MutableStateFlow(
        TestYourselfFormatUiState(
            source = route.source,
            isUnitScope = isUnitScope,
            unitId = route.unitId,
            conceptId = route.conceptId
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.linkedChildGrade
                .flatMapLatest { grade -> getConceptsUseCase(grade = grade, board = "CBSE") }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { all ->
                    if (isUnitScope) {
                        val match = all.firstOrNull { it.unitId == route.unitId }
                        if (match == null) {
                            _uiState.update { it.copy(isLoading = false, error = "Lesson not found") }
                        } else {
                            _uiState.update {
                                it.copy(
                                    title = match.unit,
                                    subject = match.subject,
                                    subjectColor = match.subjectColor,
                                    subjectIcon = match.subjectIcon,
                                    unitName = match.unit,
                                    isLoading = false
                                )
                            }
                            maybePrefetchCuratedQuiz(all)
                        }
                    } else {
                        val match = all.firstOrNull { it.id == route.conceptId }
                        if (match == null) {
                            _uiState.update { it.copy(isLoading = false, error = "Concept not found") }
                        } else {
                            _uiState.update {
                                it.copy(
                                    title = match.name,
                                    subject = match.subject,
                                    subjectColor = match.subjectColor,
                                    subjectIcon = match.subjectIcon,
                                    unitId = match.unitId,
                                    unitName = match.unit,
                                    isLoading = false
                                )
                            }
                            maybePrefetchCuratedQuiz(all)
                        }
                    }
                }
        }
    }

    /**
     * Speculative pre-generation. The moment the user lands on the format
     * picker for "Our Question Set", we start generating the 10-question
     * curated quiz in the background. By the time they tap MCQ → Quiz, the
     * answer is already in the cache and the next screen renders instantly.
     */
    private fun maybePrefetchCuratedQuiz(allConcepts: List<Concept>) {
        if (_uiState.value.source != SOURCE_CURATED) return
        val targetId = if (isUnitScope) route.unitId else route.conceptId
        if (targetId.isBlank()) return
        // Hand-written bank already covers it — no fallback needed.
        val bankHit = if (isUnitScope) {
            getCuratedQuestionsUseCase(targetId)
        } else {
            getCuratedQuestionsUseCase.forConcept(targetId)
        }
        if (bankHit.isNotEmpty()) return
        // Already cached from a previous visit.
        if (curatedQuizCache.get(targetId) != null) return

        viewModelScope.launch {
            runCatching {
                val grade = userRepository.linkedChildGrade.first()
                val (subject, unitName, conceptName, description) = if (isUnitScope) {
                    val unitConcepts = allConcepts.filter { it.unitId == route.unitId }
                    val first = unitConcepts.firstOrNull() ?: return@runCatching
                    PrefetchScope(
                        first.subject,
                        first.unit,
                        "",
                        unitConcepts.joinToString("\n\n") { c -> "• ${c.name}: ${c.description}" }
                    )
                } else {
                    val concept = allConcepts.firstOrNull { it.id == route.conceptId }
                        ?: return@runCatching
                    PrefetchScope(
                        concept.subject,
                        concept.unit,
                        concept.name,
                        concept.description
                    )
                }
                mcqGenerationRepository.generateFromLesson(
                    subject = subject,
                    grade = grade,
                    unitName = unitName,
                    conceptName = conceptName,
                    conceptDescription = description,
                    targetCount = 10
                ).onSuccess { curatedQuizCache.put(targetId, it) }
                    .onFailure { Log.w("TestYourselfPrefetch", "Prefetch failed", it) }
            }.onFailure {
                Log.w("TestYourselfPrefetch", "Prefetch threw", it)
            }
        }
    }

    private data class PrefetchScope(
        val subject: String,
        val unitName: String,
        val conceptName: String,
        val description: String
    )

    /**
     * Populates [AiPracticeContextHolder] with the picked lesson/concept so
     * the existing [com.dobi.app.feature.writeanswer.WriteAnswerScreen] flow
     * (which keys off this holder) can be reused for Test Yourself
     * Subjective practice.
     *
     * Returns true on success. Suspends because we read child grade.
     */
    suspend fun prepareSubjectiveContext(): Boolean {
        val state = _uiState.value
        if (state.title.isBlank()) return false
        val grade = userRepository.linkedChildGrade.first()
        val (displayName, scopeText, key) = if (state.isUnitScope) {
            Triple(
                state.unitName,
                "Lesson: ${state.unitName}",
                "ty_unit_${state.unitId}"
            )
        } else {
            val parent = state.unitName.ifBlank { state.title }
            Triple(
                state.title,
                "Lesson: $parent\nConcept (focus narrowly on this): ${state.title}",
                "ty_concept_${state.conceptId}"
            )
        }
        // Curated → Dobi generates questions from syllabus, student writes
        //           answers per Q.
        // AI       → student snaps textbook/question pages, Dobi generates
        //           questions FROM those photos (scope-validated), then
        //           student writes & snaps each answer.
        val mode = if (state.isAi) PreselectedWriteMode.AI_TEXTBOOK
                   else PreselectedWriteMode.GENERATED
        aiPracticeContextHolder.put(
            AiPracticeContext(
                selectionKey = key,
                displayName = displayName,
                subject = state.subject,
                grade = grade,
                scopeText = scopeText,
                preselectedWriteMode = mode,
                unitId = state.unitId,
                unitName = state.unitName,
                conceptId = if (state.isUnitScope) "" else state.conceptId,
                conceptName = if (state.isUnitScope) "" else state.title
            )
        )
        return true
    }
}
