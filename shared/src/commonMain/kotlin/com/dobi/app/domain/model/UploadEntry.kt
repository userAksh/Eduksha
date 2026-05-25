package com.dobi.app.domain.model

/**
 * A single upload posted by a child. Aggregates everything needed to surface
 * the upload on the Revisit Past Topics flow: subject + lesson/concept/topic
 * binding, photos, kind, and timestamp. Stored in Firestore under the
 * `uploads` collection, with photo files in Firebase Storage under
 * `uploads/{childId}/`.
 */
data class UploadEntry(
    val id: String = "",
    val childId: String = "",
    /** kind matches WorkUploadKind.key (class_work, self_learning, tuition_*). */
    val kind: String = "",
    val subject: String = "",
    val subjectColor: String = "",
    val subjectIcon: String = "",
    /** Empty for tuition uploads (no syllabus binding). */
    val unitId: String = "",
    val unitName: String = "",
    val conceptId: String = "",
    val conceptName: String = "",
    /** Free-form topic name written by the child (tuition flow). */
    val topic: String = "",
    val photoUrls: List<String> = emptyList(),
    val timestamp: Long = 0L
) {
    /** Title shown on the Revisit topic card. */
    val displayTitle: String
        get() = when {
            topic.isNotBlank() -> topic
            conceptName.isNotBlank() -> conceptName
            unitName.isNotBlank() -> unitName
            else -> "Untitled"
        }

    /** Stable group key — one card per group on the topic list. */
    val groupKey: String
        get() = when {
            topic.isNotBlank() -> "tuition:${subject.lowercase()}:${topic.lowercase()}"
            conceptId.isNotBlank() -> "concept:$conceptId"
            unitId.isNotBlank() -> "unit:$unitId"
            else -> "unknown:$id"
        }

    val isTuition: Boolean get() = kind.startsWith("tuition_")
}
