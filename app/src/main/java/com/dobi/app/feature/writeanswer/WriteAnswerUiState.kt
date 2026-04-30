package com.dobi.app.feature.writeanswer

import com.dobi.app.domain.model.GradingResult
import com.dobi.app.domain.model.SubjectiveQuestion

enum class WritePhase {
    LOADING,
    MODE_PICKER,
    COLLECTING_NOTEBOOK,
    COLLECTING_TEXTBOOK,
    GENERATING_QUESTIONS,
    COLLECTING_PER_QUESTION,
    GRADING,
    RESULTS,
    ERROR
}

enum class WriteMode { NOTEBOOK, GENERATED, AI_FROM_TEXTBOOK }

data class WriteAnswerUiState(
    val phase: WritePhase = WritePhase.LOADING,
    val mode: WriteMode? = null,
    val displayName: String = "",
    val subject: String = "",
    val notebookPhotoUris: List<String> = emptyList(),
    val textbookPhotoUris: List<String> = emptyList(),
    val questions: List<SubjectiveQuestion> = emptyList(),
    val photosByQuestionId: Map<String, List<String>> = emptyMap(),
    val result: GradingResult? = null,
    /**
     * False when the previous screen forced a specific mode (e.g. Test
     * Yourself's source already implied notebook vs generated). The
     * collection screens hide their "Change mode" affordance in that case.
     */
    val canChangeMode: Boolean = true,
    val error: String? = null
) {
    val canSubmitNotebook: Boolean
        get() = phase == WritePhase.COLLECTING_NOTEBOOK && notebookPhotoUris.isNotEmpty()

    val canSubmitTextbook: Boolean
        get() = phase == WritePhase.COLLECTING_TEXTBOOK && textbookPhotoUris.isNotEmpty()

    val canSubmitGenerated: Boolean
        get() = phase == WritePhase.COLLECTING_PER_QUESTION &&
            photosByQuestionId.any { it.value.isNotEmpty() }
}
