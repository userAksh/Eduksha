package com.dobi.app.feature.parenttask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.analytics.AnalyticsTracker
import com.dobi.app.analytics.Events
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.GeneratedQuizCache
import com.dobi.app.data.repository.McqGenerationRepository
import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.domain.repository.ParentTaskRepository
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DoParentTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ParentTaskRepository,
    private val getConceptsUseCase: GetConceptsUseCase,
    private val userRepository: UserRepository,
    private val mcqGenerationRepository: McqGenerationRepository,
    private val generatedQuizCache: GeneratedQuizCache,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val taskId: String = savedStateHandle.toRoute<Screen.DoParentTask>().taskId

    private val _uiState = MutableStateFlow(DoParentTaskUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            repository.getById(taskId).fold(
                onSuccess = { task ->
                    _uiState.update { it.copy(task = task, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't load")
                    }
                }
            )
        }
    }

    fun onPhotosAdded(uris: List<String>) {
        _uiState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris + uris) }
    }

    fun onPhotoRemoved(uri: String) {
        _uiState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris - uri) }
    }

    fun submitPhotos() {
        if (!_uiState.value.canSubmitPhotos) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            repository.submitWithLocalPhotos(taskId, _uiState.value.selectedPhotoUris).fold(
                onSuccess = {
                    analytics.log(Events.TASK_SUBMITTED) {
                        putString("task_type", _uiState.value.task?.type?.name ?: "PHOTO")
                    }
                    _uiState.update { it.copy(isSubmitting = false, finished = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = e.message ?: "Couldn't submit. Try again."
                        )
                    }
                }
            )
        }
    }

    /** OUR_QUESTIONS path: generate the MCQs, push to cache, signal nav. */
    fun startQuiz() {
        val task = _uiState.value.task ?: return
        if (task.type != ParentTaskType.OUR_QUESTIONS || _uiState.value.isPreparingQuiz) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingQuiz = true, error = null) }
            val grade = userRepository.linkedChildGrade.first()
            val concepts = getConceptsUseCase(grade = grade, board = "CBSE").first()
            val (subject, unitName, conceptName, description) = if (task.isUnitScope) {
                val unitConcepts = concepts.filter { it.unitId == task.unitId }
                val first = unitConcepts.firstOrNull()
                LessonScope(
                    subject = first?.subject.orEmpty(),
                    unitName = first?.unit.orEmpty(),
                    conceptName = "",
                    description = unitConcepts.joinToString("\n\n") { c ->
                        "• ${c.name}: ${c.description}"
                    }
                )
            } else {
                val concept = concepts.firstOrNull { it.id == task.conceptId }
                LessonScope(
                    subject = concept?.subject.orEmpty(),
                    unitName = concept?.unit.orEmpty(),
                    conceptName = concept?.name.orEmpty(),
                    description = concept?.description.orEmpty()
                )
            }
            mcqGenerationRepository.generateFromLesson(
                subject = subject,
                grade = grade,
                unitName = unitName,
                conceptName = conceptName,
                conceptDescription = description,
                targetCount = task.mcqCount
            ).fold(
                onSuccess = { questions ->
                    val cacheKey = "parent_task_$taskId"
                    generatedQuizCache.put(cacheKey, questions)
                    _uiState.update {
                        it.copy(isPreparingQuiz = false, launchQuizCacheKey = cacheKey)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isPreparingQuiz = false,
                            error = e.message ?: "Couldn't prepare the quiz."
                        )
                    }
                }
            )
        }
    }

    fun onQuizLaunchConsumed() {
        _uiState.update { it.copy(launchQuizCacheKey = null) }
    }

    /**
     * Called by NavGraph after the QuizScreen has reported the score back via
     * the saved-state handle. Writes submission to Firestore.
     */
    fun submitQuizScore(score: Int, total: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            repository.submitByChild(taskId, emptyList(), score, total).fold(
                onSuccess = {
                    analytics.log(Events.TASK_SUBMITTED) { putString("task_type", "OUR_QUESTIONS") }
                    _uiState.update { it.copy(isSubmitting = false, finished = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = e.message ?: "Couldn't save score"
                        )
                    }
                }
            )
        }
    }

    private data class LessonScope(
        val subject: String,
        val unitName: String,
        val conceptName: String,
        val description: String
    )
}
