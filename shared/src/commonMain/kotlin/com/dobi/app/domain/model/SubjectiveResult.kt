package com.dobi.app.domain.model

/**
 * A persisted record of one Write Answers / Subjective practice session.
 * Stored in Firestore so the child (and parents) can revisit past answers
 * with Dobi's verdict from the Self Revision flow.
 */
data class SubjectiveResult(
    val id: String = "",
    val childId: String = "",
    /** "unit" or "concept" — same convention as [RevisionScope]. */
    val scope: String = "",
    /** unitId / conceptId the practice session was tied to. */
    val targetId: String = "",
    val displayName: String = "",
    val subject: String = "",
    val unitId: String = "",
    val unitName: String = "",
    val conceptId: String = "",
    val conceptName: String = "",
    val timestamp: Long = 0L,
    val attemptedCount: Int = 0,
    val correctCount: Int = 0,
    val accuracyPct: Int = 0,
    /** Each entry pairs the question with the child's answer + Dobi's verdict. */
    val items: List<SubjectiveResultItem> = emptyList()
)

data class SubjectiveResultItem(
    val questionId: String = "",
    val questionText: String = "",
    val answerSeen: String = "",
    val isAttempted: Boolean = false,
    val isCorrect: Boolean = false,
    val score: Float = 0f,
    val softVerdict: String = "",
    val strictFeedback: String = ""
)
