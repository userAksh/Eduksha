package com.dobi.app.feature.writeanswer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.AiPracticeContextHolder
import com.dobi.app.data.repository.GeneratedQuizCache
import com.dobi.app.data.repository.PreselectedWriteMode
import com.dobi.app.data.repository.SubjectiveResultRepository
import com.dobi.app.domain.model.GradingResult
import com.dobi.app.domain.model.SubjectiveQuestion
import com.dobi.app.domain.model.SubjectiveResult
import com.dobi.app.domain.model.SubjectiveResultItem
import com.dobi.app.domain.usecase.GenerateMcqFromImageUseCase
import com.dobi.app.domain.usecase.GenerateMcqRequest
import com.dobi.app.domain.usecase.GenerateSubjectiveFromImagesUseCase
import com.dobi.app.domain.usecase.GenerateSubjectiveQuestionsUseCase
import com.dobi.app.domain.usecase.GradeNotebookAnswersUseCase
import com.dobi.app.domain.usecase.GradeStructuredAnswersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WriteAnswerNavEvent {
    data object LaunchAntiCheatQuiz : WriteAnswerNavEvent()
    data object Back : WriteAnswerNavEvent()
}

@HiltViewModel
class WriteAnswerViewModel @Inject constructor(
    private val aiPracticeContextHolder: AiPracticeContextHolder,
    private val generateQuestions: GenerateSubjectiveQuestionsUseCase,
    private val generateQuestionsFromImages: GenerateSubjectiveFromImagesUseCase,
    private val gradeStructured: GradeStructuredAnswersUseCase,
    private val gradeNotebook: GradeNotebookAnswersUseCase,
    private val generateMcqUseCase: GenerateMcqFromImageUseCase,
    private val generatedQuizCache: GeneratedQuizCache,
    private val subjectiveResultRepository: SubjectiveResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WriteAnswerUiState())
    val uiState = _uiState.asStateFlow()

    private val _nav = Channel<WriteAnswerNavEvent>(Channel.BUFFERED)
    val navEvents = _nav.receiveAsFlow()

    init {
        val ctx = aiPracticeContextHolder.peek()
        if (ctx == null) {
            _uiState.update {
                it.copy(
                    phase = WritePhase.ERROR,
                    error = "Selection lost. Please go back and pick topics again."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    phase = WritePhase.MODE_PICKER,
                    displayName = ctx.displayName,
                    subject = ctx.subject,
                    canChangeMode = ctx.preselectedWriteMode == null
                )
            }
            // When the previous screen has already implied a mode (e.g.
            // Test Yourself: curated → generated, AI → notebook), skip the
            // mode picker and drop the child straight into that mode.
            when (ctx.preselectedWriteMode) {
                PreselectedWriteMode.GENERATED -> pickGeneratedMode()
                PreselectedWriteMode.NOTEBOOK -> pickNotebookMode()
                PreselectedWriteMode.AI_TEXTBOOK -> pickAiTextbookMode()
                null -> Unit
            }
        }
    }

    fun pickNotebookMode() {
        _uiState.update { it.copy(mode = WriteMode.NOTEBOOK, phase = WritePhase.COLLECTING_NOTEBOOK) }
    }

    fun pickGeneratedMode() {
        _uiState.update { it.copy(mode = WriteMode.GENERATED, phase = WritePhase.GENERATING_QUESTIONS) }
        val ctx = aiPracticeContextHolder.peek() ?: return
        viewModelScope.launch {
            val targetCount = computeQuestionCount(ctx.scopeText)
            generateQuestions(ctx.scopeText, ctx.subject, ctx.grade, targetCount).fold(
                onSuccess = { qs ->
                    _uiState.update {
                        it.copy(
                            questions = qs,
                            phase = WritePhase.COLLECTING_PER_QUESTION
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Question gen failed", e)
                    _uiState.update {
                        it.copy(phase = WritePhase.ERROR, error = friendlyError(e.message))
                    }
                }
            )
        }
    }

    private fun computeQuestionCount(scopeText: String): Int {
        val lessons = scopeText.lineSequence().count { it.startsWith("Lesson:") }
        val concepts = scopeText.lineSequence().count {
            it.startsWith("Concept (focus") || it.startsWith("Concepts (focus")
        }
        // Concepts line lists 1-2 concept names; treat the line as N concepts if comma-separated.
        val conceptCount = scopeText.lineSequence()
            .filter { it.startsWith("Concept (focus") || it.startsWith("Concepts (focus") }
            .map { it.substringAfter(":").split(",").size }
            .sum()
        return when {
            conceptCount == 1 -> 3
            conceptCount >= 2 -> 5
            lessons >= 2 -> 8
            else -> 5  // 1 lesson
        }
    }

    fun onNotebookPhotosAdded(uris: List<String>) {
        _uiState.update { it.copy(notebookPhotoUris = it.notebookPhotoUris + uris) }
    }

    fun onNotebookPhotoRemoved(uri: String) {
        _uiState.update { it.copy(notebookPhotoUris = it.notebookPhotoUris - uri) }
    }

    fun pickAiTextbookMode() {
        _uiState.update {
            it.copy(mode = WriteMode.AI_FROM_TEXTBOOK, phase = WritePhase.COLLECTING_TEXTBOOK)
        }
    }

    fun onTextbookPhotosAdded(uris: List<String>) {
        _uiState.update { it.copy(textbookPhotoUris = it.textbookPhotoUris + uris) }
    }

    fun onTextbookPhotoRemoved(uri: String) {
        _uiState.update { it.copy(textbookPhotoUris = it.textbookPhotoUris - uri) }
    }

    fun submitTextbook() {
        val state = _uiState.value
        if (!state.canSubmitTextbook) return
        val ctx = aiPracticeContextHolder.peek() ?: return
        _uiState.update { it.copy(phase = WritePhase.GENERATING_QUESTIONS, error = null) }
        viewModelScope.launch {
            val targetCount = computeQuestionCount(ctx.scopeText)
            generateQuestionsFromImages(
                scopeText = ctx.scopeText,
                subject = ctx.subject,
                grade = ctx.grade,
                photoUris = state.textbookPhotoUris,
                targetCount = targetCount
            ).fold(
                onSuccess = { qs ->
                    _uiState.update {
                        it.copy(
                            questions = qs,
                            phase = WritePhase.COLLECTING_PER_QUESTION
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Image question gen failed", e)
                    _uiState.update {
                        it.copy(
                            phase = WritePhase.COLLECTING_TEXTBOOK,
                            error = friendlyError(e.message)
                        )
                    }
                }
            )
        }
    }

    fun onPerQuestionPhotoAdded(questionId: String, uri: String) {
        _uiState.update { state ->
            val existing = state.photosByQuestionId[questionId].orEmpty()
            state.copy(
                photosByQuestionId = state.photosByQuestionId + (questionId to (existing + uri))
            )
        }
    }

    fun onPerQuestionPhotoRemoved(questionId: String, uri: String) {
        _uiState.update { state ->
            val existing = state.photosByQuestionId[questionId].orEmpty() - uri
            state.copy(photosByQuestionId = state.photosByQuestionId + (questionId to existing))
        }
    }

    fun submitNotebook() {
        val state = _uiState.value
        if (!state.canSubmitNotebook) return
        val ctx = aiPracticeContextHolder.peek() ?: return
        _uiState.update { it.copy(phase = WritePhase.GRADING, error = null) }
        viewModelScope.launch {
            gradeNotebook(ctx.scopeText, ctx.subject, ctx.grade, state.notebookPhotoUris).fold(
                onSuccess = { res ->
                    if (!res.isOnTopicOverall) {
                        _uiState.update {
                            it.copy(
                                phase = WritePhase.COLLECTING_NOTEBOOK,
                                error = res.offTopicReason.ifBlank {
                                    "These notebook pages don't look like they're from " +
                                        "'${state.displayName}'. Please re-snap the pages " +
                                        "where you wrote answers for this lesson."
                                }
                            )
                        }
                    } else {
                        _uiState.update { it.copy(phase = WritePhase.RESULTS, result = res) }
                        persistSubjectiveResult(state.questions, res)
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Notebook grading failed", e)
                    _uiState.update {
                        it.copy(phase = WritePhase.COLLECTING_NOTEBOOK, error = friendlyError(e.message))
                    }
                }
            )
        }
    }

    fun submitGenerated() {
        val state = _uiState.value
        if (!state.canSubmitGenerated) return
        val ctx = aiPracticeContextHolder.peek() ?: return
        _uiState.update { it.copy(phase = WritePhase.GRADING, error = null) }
        viewModelScope.launch {
            gradeStructured(
                ctx.scopeText, ctx.subject, ctx.grade,
                state.questions, state.photosByQuestionId
            ).fold(
                onSuccess = { res ->
                    if (!res.isOnTopicOverall) {
                        _uiState.update {
                            it.copy(
                                phase = WritePhase.COLLECTING_PER_QUESTION,
                                error = res.offTopicReason.ifBlank {
                                    "These answers don't look like they're from '${state.displayName}'."
                                }
                            )
                        }
                    } else {
                        _uiState.update { it.copy(phase = WritePhase.RESULTS, result = res) }
                        persistSubjectiveResult(state.questions, res)
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Structured grading failed", e)
                    _uiState.update {
                        it.copy(phase = WritePhase.COLLECTING_PER_QUESTION, error = friendlyError(e.message))
                    }
                }
            )
        }
    }

    private fun persistSubjectiveResult(
        questions: List<SubjectiveQuestion>,
        result: GradingResult
    ) {
        val ctx = aiPracticeContextHolder.peek() ?: return
        // Only surface in Revisit when scoped to a curated lesson/concept
        // (Test Yourself path). Class Test Prep populates AiPracticeContext
        // for free-form selections and shouldn't pollute Revisit.
        if (ctx.unitId.isBlank() && ctx.conceptId.isBlank()) return

        val byId = questions.associateBy { it.id }
        val items = result.grades.map { g ->
            val q = byId[g.questionId]
            SubjectiveResultItem(
                questionId = g.questionId,
                questionText = q?.text ?: g.questionSeen,
                answerSeen = g.answerSeen,
                isAttempted = g.isAttempted,
                isCorrect = g.isCorrect,
                score = g.score,
                softVerdict = g.softVerdict,
                strictFeedback = g.strictFeedback
            )
        }
        val accuracyPct = if (result.attemptedCount == 0) 0
        else ((result.correctCount.toFloat() / result.attemptedCount) * 100f).toInt()

        viewModelScope.launch {
            subjectiveResultRepository.save(
                SubjectiveResult(
                    scope = if (ctx.conceptId.isNotBlank()) "concept" else "unit",
                    targetId = if (ctx.conceptId.isNotBlank()) ctx.conceptId else ctx.unitId,
                    displayName = ctx.displayName,
                    subject = ctx.subject,
                    unitId = ctx.unitId,
                    unitName = ctx.unitName,
                    conceptId = ctx.conceptId,
                    conceptName = ctx.conceptName,
                    attemptedCount = result.attemptedCount,
                    correctCount = result.correctCount,
                    accuracyPct = accuracyPct,
                    items = items
                )
            ).onFailure { Log.w(TAG, "Subjective save failed", it) }
        }
    }

    fun launchAntiCheatQuiz() {
        val state = _uiState.value
        val ctx = aiPracticeContextHolder.peek() ?: return
        val photos = when (state.mode) {
            WriteMode.NOTEBOOK -> state.notebookPhotoUris
            WriteMode.AI_FROM_TEXTBOOK -> {
                // Anti-cheat MCQ should re-quiz from the textbook pages they
                // generated questions from, not the per-question answer photos.
                state.textbookPhotoUris.ifEmpty {
                    state.photosByQuestionId.values.flatten().distinct()
                }
            }
            else -> state.photosByQuestionId.values.flatten().distinct()
        }
        if (photos.isEmpty()) return

        _uiState.update { it.copy(phase = WritePhase.GRADING) }
        viewModelScope.launch {
            generateMcqUseCase(
                GenerateMcqRequest(
                    unitId = "",
                    unitName = ctx.displayName,
                    subject = ctx.subject,
                    grade = ctx.grade,
                    photoUris = photos,
                    targetCount = 5,
                    customScopeText = ctx.scopeText
                )
            ).fold(
                onSuccess = { mcqs ->
                    generatedQuizCache.put(ANTI_CHEAT_KEY, mcqs)
                    _uiState.update { it.copy(phase = WritePhase.RESULTS) }
                    _nav.send(WriteAnswerNavEvent.LaunchAntiCheatQuiz)
                },
                onFailure = { e ->
                    Log.e(TAG, "Anti-cheat MCQ gen failed", e)
                    _uiState.update {
                        it.copy(phase = WritePhase.RESULTS, error = friendlyError(e.message))
                    }
                }
            )
        }
    }

    fun finish() {
        viewModelScope.launch { _nav.send(WriteAnswerNavEvent.Back) }
    }

    fun goBackToModePicker() {
        _uiState.update {
            it.copy(
                phase = WritePhase.MODE_PICKER,
                mode = null,
                error = null,
                notebookPhotoUris = emptyList(),
                textbookPhotoUris = emptyList(),
                questions = emptyList(),
                photosByQuestionId = emptyMap()
            )
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    private fun friendlyError(raw: String?): String {
        val msg = raw.orEmpty().trim()
        val lower = msg.lowercase()
        return when {
            "unavailable" in lower || "overloaded" in lower || "503" in lower ->
                "Dobi's brain is busy right now. Please try again in a minute."
            "resource_exhausted" in lower || "quota" in lower ->
                "We've hit today's quiz limit. Try again in a bit."
            "no internet" in lower || "unable to connect" in lower || "unable to resolve" in lower ->
                "No internet right now. Check your connection and try again."
            "no photos provided" in lower -> "Please add at least one photo first."
            "couldn't read uploaded photos" in lower -> "We couldn't open those photos. Re-add them."
            msg.isNotBlank() -> msg
            else -> "Something went wrong. Please try again."
        }
    }

    companion object {
        private const val TAG = "WriteAnsVM"
        const val ANTI_CHEAT_KEY = "wa_anticheat"
    }
}
