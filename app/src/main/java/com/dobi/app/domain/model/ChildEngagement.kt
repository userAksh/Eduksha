package com.dobi.app.domain.model

/**
 * Per-child engagement snapshot for the parent dashboard tiles.
 *
 * - [daysActiveThisWeek] is a count (0–7) of distinct calendar days, in the
 *   user's local time zone, from this Monday through today on which the child
 *   logged at least one activity (upload, subjective answer, or quiz attempt).
 * - [isActiveToday] mirrors the same activity sources but scoped to today.
 * - [accuracyPct] is the pooled all-time MCQ + subjective accuracy, 0–100,
 *   or null when the child has no scored attempts yet.
 *
 * Computed live from `uploads`, `subjective_results`, and the
 * `quiz_attempts/{childId}` subtree — there is no stored streak counter.
 */
data class ChildEngagement(
    val childId: String = "",
    val daysActiveThisWeek: Int = 0,
    val isActiveToday: Boolean = false,
    val accuracyPct: Int? = null
)
