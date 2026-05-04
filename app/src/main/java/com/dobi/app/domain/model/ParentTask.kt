package com.dobi.app.domain.model

/**
 * A task contract between parent and child:
 *   1. Child requests a topic                              → REQUESTED
 *   2. Parent picks a [ParentTaskType] + assigns           → ASSIGNED
 *   3. Child does the work, submits artefacts              → PENDING_REVIEW
 *   4. Parent accepts                                      → ACCEPTED
 *   4b. Parent rejects, child redoes                       → ASSIGNED (loop)
 *
 * Stored in Firestore at `parentTaskRequests/{id}`.
 */
data class ParentTask(
    val id: String = "",
    val parentId: String = "",
    val childId: String = "",
    // Scope picked by the child
    val unitId: String = "",
    val unitName: String = "",
    val conceptId: String = "",
    val conceptName: String = "",
    val isUnitScope: Boolean = true,
    val subject: String = "",
    val subjectColor: String = "",
    val subjectIcon: String = "",
    // Assigned by parent
    val type: ParentTaskType? = null,
    val customNote: String = "",
    val mcqCount: Int = 5,
    // Submitted by child
    val submissionPhotoUrls: List<String> = emptyList(),
    val submissionScore: Int? = null,
    val submissionTotal: Int? = null,
    // Reviewed by parent
    val parentReviewNote: String = "",
    val status: ParentTaskStatus = ParentTaskStatus.REQUESTED,
    val requestedAtMillis: Long = 0L,
    val assignedAtMillis: Long = 0L,
    val submittedAtMillis: Long = 0L,
    val reviewedAtMillis: Long = 0L
) {
    val displayName: String
        get() = if (isUnitScope) unitName else conceptName
}

enum class ParentTaskType {
    REVISE_CHAPTER,    // child uploads photos of revision notes
    WRITE_QA,          // child writes Q&A in book + uploads image
    OUR_QUESTIONS,     // 5 MCQs (concept) or 10 MCQs (lesson)
    CUSTOM             // parent free-text + child uploads photo proof
}

enum class ParentTaskStatus {
    REQUESTED,
    ASSIGNED,
    PENDING_REVIEW,
    ACCEPTED,
    REJECTED
}
