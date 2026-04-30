package com.dobi.app.domain.model

/**
 * A topic the child has flagged for later revision. Surfaced on the
 * Self Revision → Need Revision flow.
 */
data class RevisionMark(
    val id: String = "",
    val childId: String = "",
    /** UNIT or CONCEPT — see RevisionScope. */
    val scope: String = "",
    val targetId: String = "",
    val name: String = "",
    val subject: String = "",
    val subjectColor: String = "",
    val subjectIcon: String = "",
    val unitId: String = "",
    val unitName: String = "",
    val markedAt: Long = 0L
)

enum class RevisionScope(val key: String) {
    UNIT("unit"),
    CONCEPT("concept");

    companion object {
        fun fromKey(key: String): RevisionScope =
            values().firstOrNull { it.key == key } ?: CONCEPT
    }
}
