package com.dobi.app.domain.model

/**
 * Aggregated insights snapshot for a single child within a chosen time range.
 *
 * Computed from `uploads`, `subjective_results`, `quiz_attempts/{childId}` and
 * `parentTaskRequests` — same activity sources that drive the dashboard tiles.
 */
data class ChildInsights(
    val childId: String = "",
    val range: InsightsRange = InsightsRange.TODAY,
    val daysActive: Int = 0,
    val activitiesCompleted: Int = 0,
    val accuracyPct: Int? = null,
    val tasksDone: Int = 0,
    val subjects: List<SubjectInsight> = emptyList(),
    val recentActivities: List<ActivityItem> = emptyList()
)

/** Time window the parent is currently filtering by. */
enum class InsightsRange(val days: Int, val label: String) {
    TODAY(1, "Today"),
    WEEK(7, "1 Week"),
    MONTH(30, "1 Month")
}

/** Per-subject roll-up within the chosen range. */
data class SubjectInsight(
    val name: String,
    val emoji: String,
    val colorHex: String,
    val topicsCount: Int,
    val activitiesCount: Int,
    val accuracyPct: Int?
)

/** One row in the recent-activity timeline. */
data class ActivityItem(
    val kind: ActivityKind,
    val title: String,
    val subject: String,
    val subjectEmoji: String,
    val subjectColorHex: String,
    val timestampMillis: Long,
    val accuracyPct: Int? = null
)

enum class ActivityKind { QUIZ, UPLOAD, WRITTEN }
