package com.dobi.app.feature.classtestprep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.AiPracticeContext
import com.dobi.app.data.repository.AiPracticeContextHolder
import com.dobi.app.data.repository.GeneratedQuizCache
import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import com.dobi.app.domain.usecase.GetCuratedQuestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class TestPrepNavEvent {
    data class StartMcqQuiz(val key: String, val displayName: String) : TestPrepNavEvent()
    data object StartAiPracticeUpload : TestPrepNavEvent()
    data object StartWriteAnswers : TestPrepNavEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ClassTestPrepViewModel @Inject constructor(
    private val getConceptsUseCase: GetConceptsUseCase,
    private val getCuratedQuestionsUseCase: GetCuratedQuestionsUseCase,
    private val generatedQuizCache: GeneratedQuizCache,
    private val aiPracticeContextHolder: AiPracticeContextHolder,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _navEvents = Channel<TestPrepNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    private val _uiState = MutableStateFlow(ClassTestPrepUiState())
    val uiState = _uiState.asStateFlow()

    private var allConcepts: List<Concept> = emptyList()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            userRepository.linkedChildGrade
                .flatMapLatest { grade -> getConceptsUseCase(grade = grade, board = "CBSE") }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { concepts ->
                    allConcepts = concepts
                    val merged = mergeWithDefaults(concepts)
                    val initial = merged.firstOrNull()?.name
                    _uiState.update {
                        it.copy(
                            subjects = merged,
                            selectedSubject = initial,
                            units = buildUnits(concepts, initial),
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun mergeWithDefaults(concepts: List<Concept>): List<TestPrepSubject> {
        val live = concepts
            .groupBy { it.subject }
            .mapValues { (_, list) ->
                val first = list.first()
                TestPrepSubject(
                    name = first.subject,
                    color = first.subjectColor,
                    icon = first.subjectIcon
                )
            }
        // Seed any presets that aren't in Firestore yet so children see the full subject row.
        val seeded = DEFAULT_SUBJECTS.map { default -> live[default.name] ?: default }
        // Preserve any extra subjects only present in Firestore.
        val extras = live.values.filter { l -> DEFAULT_SUBJECTS.none { it.name == l.name } }
        return seeded + extras.sortedBy { it.name }
    }

    private fun buildUnits(concepts: List<Concept>, subject: String?): List<TestPrepUnit> {
        if (subject == null) return emptyList()
        return concepts
            .filter { it.subject == subject }
            .groupBy { it.unitId }
            .entries
            .sortedBy { it.value.first().unitOrder }
            .map { (unitId, ucs) ->
                TestPrepUnit(
                    unitId = unitId,
                    unitName = ucs.first().unit,
                    concepts = ucs.sortedBy { it.order }
                        .map { TestPrepConcept(it.id, it.name) }
                )
            }
    }

    fun onSubjectSelected(subject: String) = _uiState.update {
        if (it.selectedSubject == subject) it
        else it.copy(
            selectedSubject = subject,
            units = buildUnits(allConcepts, subject),
            selectedItems = emptyList()
        )
    }

    fun onTeacherHonorificChanged(h: String) = _uiState.update {
        it.copy(teacherHonorific = if (it.teacherHonorific == h) "" else h)
    }

    fun onTeacherNameChanged(name: String) = _uiState.update {
        it.copy(teacherName = name)
    }

    fun onDateChanged(millis: Long) = _uiState.update {
        it.copy(dateMillis = millis)
    }

    fun onItemToggled(item: TestPrepSelection) = _uiState.update { state ->
        val existing = state.selectedItems.firstOrNull { it.id == item.id }
        if (existing != null) {
            return@update state.copy(
                selectedItems = state.selectedItems - existing,
                showLimitInfo = false,
                limitMessage = null
            )
        }

        // Rule: picking a concept while its parent lesson is already on the list
        // alongside other items would create a redundant / cross-lesson mess. Allow
        // the swap only when the parent lesson is the sole selection (lesson →
        // single concept narrowing).
        if (!item.isLesson) {
            val parentLessonSelected = state.selectedItems.firstOrNull {
                it.isLesson && it.unitId == item.unitId
            }
            if (parentLessonSelected != null && state.selectedItems.size > 1) {
                return@update state.copy(
                    showLimitInfo = true,
                    limitMessage = "\"${parentLessonSelected.title}\" is already on your list — " +
                        "it covers this concept. Deselect another item first to focus on just this concept."
                )
            }
        }

        // Rule: once a concept from any lesson is selected, the focus is locked to
        // that lesson. The child can only add another concept from the SAME lesson.
        // Picking a different lesson — or a concept from another lesson — is blocked.
        val anchorConcept = state.selectedItems.firstOrNull { !it.isLesson }
        if (anchorConcept != null && anchorConcept.unitId != item.unitId) {
            val message = if (item.isLesson) {
                "You're already revising \"${anchorConcept.title}\" from " +
                    "\"${anchorConcept.unitName}\". Deselect it first to pick a different lesson."
            } else {
                "Concepts have to come from the same lesson. " +
                    "Deselect \"${anchorConcept.title}\" or pick its whole lesson instead."
            }
            return@update state.copy(
                showLimitInfo = true,
                limitMessage = message
            )
        }

        // Hierarchical conflicts: a lesson covers all its concepts, so picking either
        // removes the other side of the relationship.
        val conflictsRemoved = state.selectedItems.filter { other ->
            when {
                // Selecting a lesson clears any concept inside that lesson.
                item.isLesson && !other.isLesson && other.unitId == item.unitId -> true
                // Selecting a concept clears its parent lesson if it was selected.
                !item.isLesson && other.isLesson && other.unitId == item.unitId -> true
                else -> false
            }
        }
        val resolved = state.selectedItems - conflictsRemoved.toSet()

        if (resolved.size >= ClassTestPrepUiState.MAX_SELECTIONS) {
            state.copy(
                showLimitInfo = true,
                limitMessage = null
            )
        } else {
            state.copy(
                selectedItems = resolved + item,
                showLimitInfo = false,
                limitMessage = null
            )
        }
    }

    fun dismissLimitInfo() = _uiState.update {
        it.copy(showLimitInfo = false, limitMessage = null)
    }

    fun onStartPractise() = _uiState.update {
        if (it.canStart) it.copy(showModePopup = true) else it
    }

    fun onModePopupDismissed() = _uiState.update { it.copy(showModePopup = false) }

    fun onPractiseAi() {
        viewModelScope.launch {
            if (!populateAiContext()) return@launch
            _navEvents.send(TestPrepNavEvent.StartAiPracticeUpload)
        }
    }

    fun onPractiseSubjective() {
        viewModelScope.launch {
            if (!populateAiContext()) return@launch
            _navEvents.send(TestPrepNavEvent.StartWriteAnswers)
        }
    }

    private suspend fun populateAiContext(): Boolean {
        val state = _uiState.value
        val items = state.selectedItems
        if (items.isEmpty()) return false

        val key = "tp_ai_" + items.map { it.id }.sorted().joinToString("_")
        val displayName = items.joinToString(" + ") { it.title }
        val subject = state.selectedSubject.orEmpty()
        val scopeText = buildScopeText(items)

        val grade = userRepository.linkedChildGrade.first()
        aiPracticeContextHolder.put(
            AiPracticeContext(
                selectionKey = key,
                displayName = displayName,
                subject = subject,
                grade = grade,
                scopeText = scopeText,
                teacherHonorific = state.teacherHonorific,
                teacherName = state.teacherName,
                dateMillis = state.dateMillis
            )
        )

        _uiState.update { it.copy(showModePopup = false) }
        return true
    }

    private fun buildScopeText(items: List<TestPrepSelection>): String {
        // Mirror the same "Lesson: ..." / "Concept (focus narrowly on this): ..."
        // structure used by the self-learning flow. Gemini is markedly better at
        // applying the lesson-match rule when it sees these explicit labels rather
        // than free-form prose.
        val lessons = items.filter { it.isLesson }
        val concepts = items.filter { !it.isLesson }
        val lines = mutableListOf<String>()

        lessons.forEach { lines += "Lesson: ${it.title}" }

        if (concepts.isNotEmpty()) {
            // Toggle rules guarantee all selected concepts share a single lesson.
            val parentLesson = concepts.first().unitName
            if (lessons.none { it.title == parentLesson }) {
                lines += "Lesson: $parentLesson"
            }
            if (concepts.size == 1) {
                lines += "Concept (focus narrowly on this): ${concepts[0].title}"
            } else {
                lines += "Concepts (focus narrowly on these): " +
                    concepts.joinToString(", ") { it.title }
            }
        }

        return lines.joinToString("\n")
    }

    fun onPractiseMcq() {
        val state = _uiState.value
        val items = state.selectedItems
        if (items.isEmpty()) return

        val pool = items.flatMap { sel ->
            if (sel.isLesson) getCuratedQuestionsUseCase(sel.id)
            else getCuratedQuestionsUseCase.forConcept(sel.id)
        }.distinctBy { it.id }.take(MAX_QUESTIONS)

        if (pool.isEmpty()) {
            _uiState.update {
                it.copy(
                    showModePopup = false,
                    showLimitInfo = true,
                    limitMessage = "Curated MCQs aren't ready for this combination yet. Try AI Practice instead."
                )
            }
            return
        }

        val key = "tp_" + items.map { it.id }.sorted().joinToString("_")
        val displayName = items.joinToString(" + ") { it.title }
        generatedQuizCache.put(key, pool)

        _uiState.update { it.copy(showModePopup = false) }
        viewModelScope.launch {
            _navEvents.send(TestPrepNavEvent.StartMcqQuiz(key = key, displayName = displayName))
        }
    }

    companion object {
        private const val MAX_QUESTIONS = 15

        private val DEFAULT_SUBJECTS = listOf(
            TestPrepSubject("Science", "#22C55E", "🔬"),
            TestPrepSubject("Mathematics", "#3B82F6", "🔢"),
            TestPrepSubject("English", "#EC4899", "📖"),
            TestPrepSubject("Social Science", "#F59E0B", "🌍"),
            TestPrepSubject("Hindi", "#EF4444", "✍️"),
            TestPrepSubject("Sanskrit", "#8B5CF6", "📜")
        )
    }
}
