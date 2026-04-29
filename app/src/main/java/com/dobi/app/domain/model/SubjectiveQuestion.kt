package com.dobi.app.domain.model

data class SubjectiveQuestion(
    val id: String,
    val text: String,
    val expectedKeywords: List<String> = emptyList(),
    val maxMarks: Int = 2
)

data class AnswerGrade(
    val questionId: String,
    val questionSeen: String = "",
    val answerSeen: String = "",
    val isAttempted: Boolean,
    val isOnTopic: Boolean,
    val score: Float,
    val isCorrect: Boolean,
    val softVerdict: String,
    val strictFeedback: String
)

data class GradingResult(
    val isOnTopicOverall: Boolean,
    val offTopicReason: String = "",
    val grades: List<AnswerGrade>
) {
    val attemptedCount: Int get() = grades.count { it.isAttempted }
    val correctCount: Int get() = grades.count { it.isCorrect }
    val accuracy: Float get() =
        if (attemptedCount == 0) 0f else correctCount.toFloat() / attemptedCount
}
