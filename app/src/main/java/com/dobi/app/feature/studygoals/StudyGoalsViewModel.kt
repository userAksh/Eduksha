package com.dobi.app.feature.studygoals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.analytics.AnalyticsTracker
import com.dobi.app.analytics.Events
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.McqGenerationRepository
import com.dobi.app.data.repository.WriteAnswerRepository
import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.model.McqQuestion
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.domain.model.SubjectiveQuestion
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.ConceptRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StudyGoalsStep { SUBJECT, TOPIC, TASK_TYPE, PREVIEW, SUBMITTING, DONE }

data class SubjectChoice(
    val name: String,
    val color: String,
    val icon: String,
)

data class TopicChoice(
    val unitId: String,
    val unitName: String,
    val subject: String,
    val subjectColor: String,
    val subjectIcon: String,
    val scopeText: String,
)

/** Sub-choice when the parent picks the Custom card. */
enum class CustomMode { WHOLE_CHAPTER, TYPED }

data class StudyGoalsUiState(
    val isLoading: Boolean = true,
    val childId: String = "",
    val childName: String = "",
    val childGrade: Int = 0,
    val childBoard: String = "",
    val blockingTask: ParentTask? = null,
    val subjects: List<SubjectChoice> = emptyList(),
    val topicsBySubject: Map<String, List<TopicChoice>> = emptyMap(),
    val step: StudyGoalsStep = StudyGoalsStep.SUBJECT,
    val selectedSubject: SubjectChoice? = null,
    val selectedTopic: TopicChoice? = null,
    val selectedType: ParentTaskType? = null,
    val customMode: CustomMode = CustomMode.WHOLE_CHAPTER,
    val customNote: String = "",
    val generatingPreview: Boolean = false,
    val previewMcqs: List<McqQuestion> = emptyList(),
    val previewSubjectiveQs: List<SubjectiveQuestion> = emptyList(),
    val previewError: String = "",
    val errorMessage: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudyGoalsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository,
    private val conceptRepository: ConceptRepository,
    private val parentTaskRepository: ParentTaskRepository,
    private val mcqGenerationRepository: McqGenerationRepository,
    private val writeAnswerRepository: WriteAnswerRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val route: Screen.StudyGoals = savedStateHandle.toRoute()
    private val childId: String = route.childId

    private val _state = MutableStateFlow(StudyGoalsUiState(childId = childId))
    val state = _state.asStateFlow()

    init {
        loadChildAndCurriculum()
        analytics.log(Events.STUDY_GOALS_OPENED)
    }

    private fun loadChildAndCurriculum() {
        val parentId = auth.currentUser?.uid.orEmpty()
        if (parentId.isBlank() || childId.isBlank()) {
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = "Couldn't identify your account.",
            )
            return
        }
        viewModelScope.launch {
            val child = childRepository.getChild(parentId, childId).getOrNull()
            if (child == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Child not found.",
                )
                return@launch
            }
            // Detect an active task — block the picker if one exists.
            val activeTask = parentTaskRepository.observeForActiveParent()
                .map { tasks ->
                    tasks.firstOrNull { it.childId == childId && it.status in ACTIVE_STATUSES }
                }
                .first()

            val concepts = conceptRepository
                .getConcepts(child.grade, child.board)
                .first()
            val (subjects, topicsBySubject) = buildCurriculum(concepts)

            _state.value = _state.value.copy(
                isLoading = false,
                childName = child.name.substringBefore(" ").ifBlank { "your child" },
                childGrade = child.grade,
                childBoard = child.board,
                blockingTask = activeTask,
                subjects = subjects,
                topicsBySubject = topicsBySubject,
            )
        }
    }

    fun selectSubject(subject: SubjectChoice) {
        _state.value = _state.value.copy(
            selectedSubject = subject,
            selectedTopic = null,
            step = StudyGoalsStep.TOPIC,
        )
    }

    fun selectTopic(topic: TopicChoice) {
        _state.value = _state.value.copy(
            selectedTopic = topic,
            step = StudyGoalsStep.TASK_TYPE,
        )
    }

    fun selectType(type: ParentTaskType) {
        _state.value = _state.value.copy(
            selectedType = type,
            previewMcqs = emptyList(),
            previewSubjectiveQs = emptyList(),
            previewError = "",
            customNote = "",
            customMode = CustomMode.WHOLE_CHAPTER,
            step = StudyGoalsStep.PREVIEW,
        )
        // Auto-kick AI generation for the question-based types.
        when (type) {
            ParentTaskType.OUR_QUESTIONS -> regenerateMcqs()
            ParentTaskType.WRITE_QA -> regenerateSubjective()
            ParentTaskType.REVISE_CHAPTER,
            ParentTaskType.CUSTOM -> { /* nothing to generate */ }
        }
    }

    fun selectCustomMode(mode: CustomMode) {
        val type = if (mode == CustomMode.WHOLE_CHAPTER) ParentTaskType.REVISE_CHAPTER
        else ParentTaskType.CUSTOM
        _state.value = _state.value.copy(customMode = mode, selectedType = type)
    }

    fun setCustomNote(note: String) {
        _state.value = _state.value.copy(customNote = note)
    }

    fun back() {
        val s = _state.value
        val previous = when (s.step) {
            StudyGoalsStep.SUBJECT -> null
            StudyGoalsStep.TOPIC -> StudyGoalsStep.SUBJECT
            StudyGoalsStep.TASK_TYPE -> StudyGoalsStep.TOPIC
            StudyGoalsStep.PREVIEW -> StudyGoalsStep.TASK_TYPE
            StudyGoalsStep.SUBMITTING,
            StudyGoalsStep.DONE -> null
        } ?: return
        _state.value = s.copy(step = previous)
    }

    fun regenerateMcqs() {
        val s = _state.value
        val topic = s.selectedTopic ?: return
        val grade = s.childGrade
        viewModelScope.launch {
            _state.value = s.copy(generatingPreview = true, previewError = "")
            val result = mcqGenerationRepository.generateFromLesson(
                subject = topic.subject,
                grade = grade,
                unitName = topic.unitName,
                conceptName = topic.unitName,
                conceptDescription = topic.scopeText,
                targetCount = 5,
            )
            val mcqs = result.getOrNull().orEmpty()
            _state.value = _state.value.copy(
                generatingPreview = false,
                previewMcqs = mcqs,
                previewError = if (result.isFailure || mcqs.isEmpty())
                    "Couldn't generate questions. Try again."
                else "",
            )
        }
    }

    fun regenerateSubjective() {
        val s = _state.value
        val topic = s.selectedTopic ?: return
        viewModelScope.launch {
            _state.value = s.copy(generatingPreview = true, previewError = "")
            val result = writeAnswerRepository.generateQuestions(
                scopeText = topic.scopeText.ifBlank { topic.unitName },
                subject = topic.subject,
                grade = s.childGrade,
                targetCount = 5,
            )
            val qs = result.getOrNull().orEmpty()
            _state.value = _state.value.copy(
                generatingPreview = false,
                previewSubjectiveQs = qs,
                previewError = if (result.isFailure || qs.isEmpty())
                    "Couldn't generate questions. Try again."
                else "",
            )
        }
    }

    fun assign() {
        val s = _state.value
        val parentId = auth.currentUser?.uid.orEmpty()
        val topic = s.selectedTopic ?: return
        val type = s.selectedType ?: return
        if (parentId.isBlank() || s.childId.isBlank()) return

        if (type == ParentTaskType.CUSTOM && s.customNote.trim().isEmpty()) {
            _state.value = s.copy(errorMessage = "Add a short instruction first.")
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(step = StudyGoalsStep.SUBMITTING, errorMessage = "")
            val result = parentTaskRepository.assignDirectlyByParent(
                parentId = parentId,
                childId = s.childId,
                unitId = topic.unitId,
                unitName = topic.unitName,
                conceptId = "",
                conceptName = "",
                isUnitScope = true,
                subject = topic.subject,
                subjectColor = topic.subjectColor,
                subjectIcon = topic.subjectIcon,
                type = type,
                customNote = if (type == ParentTaskType.CUSTOM) s.customNote.trim() else "",
                mcqCount = if (type == ParentTaskType.OUR_QUESTIONS) s.previewMcqs.size.coerceAtLeast(5) else 5,
                assignedMcqs = if (type == ParentTaskType.OUR_QUESTIONS) s.previewMcqs else emptyList(),
                assignedSubjectiveQs = if (type == ParentTaskType.WRITE_QA) s.previewSubjectiveQs else emptyList(),
            )
            if (result.isSuccess) {
                analytics.log(Events.TASK_ASSIGNED) {
                    putString("source", "study_goals")
                    putString("task_type", type.name)
                    putString("subject", topic.subject)
                }
                _state.value = _state.value.copy(step = StudyGoalsStep.DONE)
            } else {
                _state.value = _state.value.copy(
                    step = StudyGoalsStep.PREVIEW,
                    errorMessage = "Couldn't assign the task. Try again.",
                )
            }
        }
    }

    fun sendReminder() {
        val taskId = _state.value.blockingTask?.id.orEmpty()
        if (taskId.isBlank()) return
        viewModelScope.launch { parentTaskRepository.sendReminder(taskId) }
    }

    private fun buildCurriculum(
        concepts: List<Concept>
    ): Pair<List<SubjectChoice>, Map<String, List<TopicChoice>>> {
        val bySubject = concepts.groupBy { it.subject.trim() }
            .filterKeys { it.isNotBlank() }
        val subjects = bySubject.entries
            .map { (subject, list) ->
                val anchor = list.first()
                SubjectChoice(
                    name = subject,
                    color = anchor.subjectColor,
                    icon = anchor.subjectIcon,
                )
            }
            .sortedBy { it.name.lowercase() }
        val topicsBySubject = bySubject.mapValues { (subject, list) ->
            list.groupBy { it.unitId }
                .filterKeys { it.isNotBlank() }
                .map { (unitId, conceptsInUnit) ->
                    val anchor = conceptsInUnit.minByOrNull { it.order } ?: conceptsInUnit.first()
                    TopicChoice(
                        unitId = unitId,
                        unitName = anchor.unit.ifBlank { anchor.name },
                        subject = subject,
                        subjectColor = anchor.subjectColor,
                        subjectIcon = anchor.subjectIcon,
                        scopeText = conceptsInUnit
                            .joinToString(separator = "\n") { c ->
                                "${c.name}: ${c.description}".trim()
                            },
                    )
                }
                .sortedBy { it.unitName.lowercase() }
        }
        return subjects to topicsBySubject
    }

    companion object {
        private val ACTIVE_STATUSES = setOf(
            ParentTaskStatus.REQUESTED,
            ParentTaskStatus.ASSIGNED,
            ParentTaskStatus.PENDING_REVIEW,
            ParentTaskStatus.REJECTED,
        )
    }
}
