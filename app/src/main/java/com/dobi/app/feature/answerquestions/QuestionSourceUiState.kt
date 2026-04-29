package com.dobi.app.feature.answerquestions

data class QuestionSourceUiState(
    val unitName: String = "",
    val subject: String = "",
    val attemptsTaken: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val hasReachedMax: Boolean get() = attemptsTaken >= MAX_ATTEMPTS

    companion object {
        const val MAX_ATTEMPTS = 2
    }
}
