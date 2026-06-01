package com.dobi.app.feature.quiz

import com.dobi.app.domain.model.McqQuestion

enum class QuizPhase { INTRO, QUESTION, DONE }

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.INTRO,
    val unitName: String = "",
    val subject: String = "",
    val questions: List<McqQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selections: Map<Int, Int> = emptyMap(),
    val revealed: Set<Int> = emptySet(),
    val attemptsTaken: Int = 0,
    val isLoading: Boolean = true,
    val isGeneratingCurated: Boolean = false,
    val showSnapAndSend: Boolean = false,
    val error: String? = null,
    /** True when showing a parent-view result where the questions can't be
     *  reconstructed on this device; score is taken from stored attempt data. */
    val isScoreOnly: Boolean = false,
    val scoreOnlyCorrect: Int = 0,
    val scoreOnlyTotal: Int = 0,
    /** True when a parent is viewing the child's quiz result (not the child). */
    val isParentView: Boolean = false,
    val isAssigningQuiz: Boolean = false,
    /** True when the parent just assigned the quiz in this session (shows success banner). */
    val quizAssigned: Boolean = false,
    /** True when this quiz was already assigned to the child before this session opened. */
    val quizAlreadyAssigned: Boolean = false,
    /** Hides Retake and Snap & Send — set when opened from the child's profile tab. */
    val hideActions: Boolean = false,
) {
    val canRetry: Boolean get() = !isParentView && !isScoreOnly && !hideActions && attemptsTaken < MAX_ATTEMPTS

    companion object {
        const val MAX_ATTEMPTS = 2
    }
    val totalQuestions: Int get() = if (isScoreOnly) scoreOnlyTotal else questions.size
    val currentQuestion: McqQuestion? get() = questions.getOrNull(currentIndex)
    val currentSelection: Int? get() = selections[currentIndex]
    val isCurrentRevealed: Boolean get() = currentIndex in revealed
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex
    val canGoPrevious: Boolean get() = currentIndex > 0

    val score: Int get() = if (isScoreOnly) scoreOnlyCorrect else revealed.count { idx ->
        selections[idx] == questions.getOrNull(idx)?.correctIndex
    }
}
