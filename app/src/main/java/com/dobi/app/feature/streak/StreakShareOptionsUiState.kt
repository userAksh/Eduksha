package com.dobi.app.feature.streak

data class StreakShareOptionsUiState(
    val displayName: String = "",
    val subject: String = "",
    val isUnitScope: Boolean = false,
    val unitId: String = "",
    val conceptId: String = "",
    val isGeneratingQuiz: Boolean = false,
    val error: String? = null,
    /** Quiz cache key + display name to navigate into [com.dobi.app.feature.quiz.QuizScreen]. */
    val launchQuiz: LaunchQuizArgs? = null,
    /** Plain-text message to fire as an Intent.ACTION_SEND share. */
    val pendingShareText: String? = null,
    /** When true, the share completed and we should pop back to streak home. */
    val finishedShareOnly: Boolean = false
)

data class LaunchQuizArgs(
    val cacheKey: String,
    val displayName: String
)
