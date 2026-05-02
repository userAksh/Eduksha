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
    val error: String? = null
) {
    val canRetry: Boolean get() = attemptsTaken < MAX_ATTEMPTS

    companion object {
        const val MAX_ATTEMPTS = 2
    }
    val totalQuestions: Int get() = questions.size
    val currentQuestion: McqQuestion? get() = questions.getOrNull(currentIndex)
    val currentSelection: Int? get() = selections[currentIndex]
    val isCurrentRevealed: Boolean get() = currentIndex in revealed
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex
    val canGoPrevious: Boolean get() = currentIndex > 0

    val score: Int get() = revealed.count { idx ->
        selections[idx] == questions.getOrNull(idx)?.correctIndex
    }
}
