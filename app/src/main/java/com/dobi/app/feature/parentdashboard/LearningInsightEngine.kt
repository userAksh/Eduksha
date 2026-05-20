package com.dobi.app.feature.parentdashboard

import com.dobi.app.data.repository.ActivityLogRepository
import com.dobi.app.data.repository.ActivityLogRepository.ActivityLogItem
import com.dobi.app.data.repository.ActivityLogRepository.Source

/**
 * Visual tone of a [LearningInsight]. The screen maps this to a soft-pastel
 * background + accent color so the card stays calm — even in the higher-
 * severity cases, no aggressive red. See ParentDashboardScreen.tonePalette().
 */
enum class InsightTone { POSITIVE, INFO, AMBER, ORANGE, SOFT_RED }

/**
 * One parent-facing observation chosen from a priority-ranked rule set. The
 * engine surfaces ONLY the single highest-priority insight that applies,
 * keeping the dashboard quiet. See [computeLearningInsight].
 *
 * Priorities (lower = more urgent / more important to surface):
 *  1. Long revision gap on a specific topic
 *  2. Subject neglect (no activity in a subject for 7+ days)
 *  3. Homework without revision (3+ days)
 *  4. Consistency drop vs. prior week
 *  5. Long inactivity (4+ days without anything)
 *  6. Positive reinforcement (5+ study days & 2+ revision days)
 *  7. Balanced-learning highlight (≥2 BOTH days)
 */
data class LearningInsight(
    val priority: Int,
    val title: String,
    val body: String,
    val helpHint: String? = null,
    val tone: InsightTone,
)

private const val DAY_MS = 24L * 60L * 60L * 1000L

fun computeLearningInsight(
    items: List<ActivityLogItem>,
    now: Long = System.currentTimeMillis(),
): LearningInsight {
    val currentWeek = bucketWeek(items, now)
    val priorWeek = bucketWeek(items, now - 7L * DAY_MS)

    revisionGapInsight(items, now)?.let { return it }
    subjectNeglectInsight(items, now)?.let { return it }
    homeworkWithoutRevisionInsight(items, now)?.let { return it }
    consistencyDropInsight(currentWeek, priorWeek)?.let { return it }
    longInactivityInsight(items, now)?.let { return it }
    balancedDaysInsight(currentWeek)?.let { return it }
    positiveReinforcementInsight(currentWeek)?.let { return it }

    return LearningInsight(
        priority = 0,
        title = "Keep building daily habits",
        body = "Even short sessions help with retention.",
        helpHint = null,
        tone = InsightTone.INFO,
    )
}

// ── P1 — Revision gap ────────────────────────────────────────────────────────
private fun revisionGapInsight(items: List<ActivityLogItem>, now: Long): LearningInsight? {
    val revisions = items.filter(::isRevisionItem)
    if (revisions.isEmpty()) return null

    // Only consider topics the child has actually touched in the last 30 days
    // — surfacing an 8-month-old fact as "needs revision" is noise, not signal.
    val lookback30d = now - 30L * DAY_MS
    val byTopic = revisions.groupBy(::topicKey)
        .filterValues { entries -> entries.any { it.timestamp >= lookback30d } }
    if (byTopic.isEmpty()) return null

    val staleTopic = byTopic
        .mapValues { (_, entries) -> entries.maxByOrNull { it.timestamp }!! }
        .entries
        .map { (_, latest) -> latest to ((now - latest.timestamp) / DAY_MS).toInt() }
        .filter { (_, days) -> days > 5 }
        .maxByOrNull { (_, days) -> days }
        ?: return null

    val (latestItem, days) = staleTopic
    val label = displayTopicLabel(latestItem)
    val tone = when {
        days > 12 -> InsightTone.SOFT_RED
        days >= 8 -> InsightTone.ORANGE
        else -> InsightTone.AMBER
    }
    return LearningInsight(
        priority = 1,
        title = "$label may need revision",
        body = "Last revised $days days ago. Regular revision improves retention.",
        helpHint = "A short recap can bring it back.",
        tone = tone,
    )
}

// ── P2 — Subject neglect ─────────────────────────────────────────────────────
private fun subjectNeglectInsight(items: List<ActivityLogItem>, now: Long): LearningInsight? {
    val withSubject = items.filter { it.subject.isNotBlank() }
    if (withSubject.isEmpty()) return null

    val lookback60d = now - 60L * DAY_MS
    val bySubject = withSubject
        .groupBy { it.subject }
        .filterValues { entries -> entries.any { it.timestamp >= lookback60d } }

    val neglected = bySubject
        .mapValues { (_, entries) -> entries.maxOf { it.timestamp } }
        .entries
        .map { (subject, ts) -> subject to ((now - ts) / DAY_MS).toInt() }
        .filter { (_, days) -> days > 7 }
        .maxByOrNull { (_, days) -> days }
        ?: return null

    val (subject, days) = neglected
    return LearningInsight(
        priority = 2,
        title = "$subject needs attention",
        body = "No revision activity for $days days.",
        helpHint = "Even 10 minutes today can rebuild momentum.",
        tone = if (days > 14) InsightTone.ORANGE else InsightTone.AMBER,
    )
}

// ── P3 — Homework without revision ───────────────────────────────────────────
private fun homeworkWithoutRevisionInsight(
    items: List<ActivityLogItem>,
    now: Long,
): LearningInsight? {
    val cutoff = now - 3L * DAY_MS
    val homeworkRecent = items.any { isHomeworkItem(it) && it.timestamp >= cutoff }
    val revisionRecent = items.any { isRevisionItem(it) && it.timestamp >= cutoff }
    if (!homeworkRecent || revisionRecent) return null

    return LearningInsight(
        priority = 3,
        title = "Homework is regular, but revision is low",
        body = "Concepts are being completed, but not revisited.",
        helpHint = "Add a short revision to balance learning.",
        tone = InsightTone.AMBER,
    )
}

// ── P4 — Consistency drop vs. last week ──────────────────────────────────────
private fun consistencyDropInsight(
    currentWeek: List<DayActivity>,
    priorWeek: List<DayActivity>,
): LearningInsight? {
    val current = currentWeek.count { it.kind != DayKind.NONE }
    val prior = priorWeek.count { it.kind != DayKind.NONE }
    if (prior < 3 || current + 1 >= prior) return null

    val inactiveDays = currentWeek.count { it.kind == DayKind.NONE }
    return LearningInsight(
        priority = 4,
        title = "Learning consistency dropped this week",
        body = "$inactiveDays inactive ${if (inactiveDays == 1) "day" else "days"} detected.",
        helpHint = "A short daily revision can rebuild the rhythm.",
        tone = InsightTone.AMBER,
    )
}

// ── P5 — Long inactivity ─────────────────────────────────────────────────────
private fun longInactivityInsight(items: List<ActivityLogItem>, now: Long): LearningInsight? {
    val mostRecent = items.maxByOrNull { it.timestamp }
    if (mostRecent == null) {
        return LearningInsight(
            priority = 5,
            title = "No recent learning activity yet",
            body = "Once an upload or quiz is logged, it will show up here.",
            tone = InsightTone.INFO,
        )
    }
    val daysSince = ((now - mostRecent.timestamp) / DAY_MS).toInt()
    if (daysSince <= 4) return null

    return LearningInsight(
        priority = 5,
        title = "No recent learning activity detected",
        body = "$daysSince days since the last session.",
        helpHint = "A short revision can help restart momentum.",
        tone = if (daysSince > 10) InsightTone.ORANGE else InsightTone.AMBER,
    )
}

// ── P7 — Balanced learning days ──────────────────────────────────────────────
private fun balancedDaysInsight(currentWeek: List<DayActivity>): LearningInsight? {
    val balanced = currentWeek.count { it.kind == DayKind.BOTH }
    if (balanced < 2) return null

    return LearningInsight(
        priority = 7,
        title = "$balanced balanced learning days this week",
        body = "Strong mix of learning and revision.",
        tone = InsightTone.POSITIVE,
    )
}

// ── P6 — Positive reinforcement ──────────────────────────────────────────────
private fun positiveReinforcementInsight(currentWeek: List<DayActivity>): LearningInsight? {
    val studyDays = currentWeek.count { it.kind != DayKind.NONE }
    val revisionDays = currentWeek.count { it.revisions.isNotEmpty() }
    if (studyDays < 5 || revisionDays < 2) return null

    return LearningInsight(
        priority = 6,
        title = "Great learning balance this week",
        body = "Homework and revision are both consistent.",
        tone = InsightTone.POSITIVE,
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun isRevisionItem(item: ActivityLogItem): Boolean = when (item.source) {
    Source.QUIZ_MCQ, Source.QUIZ_SUBJECTIVE -> true
    Source.UPLOAD -> item.category != ActivityLogRepository.Category.HOME_WORK
    else -> false
}

private fun isHomeworkItem(item: ActivityLogItem): Boolean = when (item.source) {
    Source.HOMEWORK -> true
    Source.UPLOAD -> item.category == ActivityLogRepository.Category.HOME_WORK
    else -> false
}

private fun topicKey(item: ActivityLogItem): String = when (val d = item.detail) {
    is ActivityLogRepository.DetailRoute.Upload -> d.groupKey
    is ActivityLogRepository.DetailRoute.QuizMcq -> "quiz-mcq:${d.unitId}:${d.conceptId}"
    is ActivityLogRepository.DetailRoute.QuizSubjective -> "subjective:${d.resultId}"
    is ActivityLogRepository.DetailRoute.Homework -> "homework:${d.homeworkId}"
}

/** Try to extract a clean topic-ish label from the activity item. Falls back
 *  to subject, then to a generic "A topic". Caps length so the card title
 *  doesn't run off the screen. */
private fun displayTopicLabel(item: ActivityLogItem): String {
    val raw = item.title.takeIf { it.isNotBlank() }
        ?: item.subject.takeIf { it.isNotBlank() }
        ?: return "A topic"
    // Many quiz / write-answer titles look like "Maths — Fractions"; prefer
    // the right-hand side which is usually the topic.
    val after = raw.substringAfterLast("—", missingDelimiterValue = raw).trim()
    val cleaned = if (after.isNotBlank()) after else raw
    return if (cleaned.length > 32) cleaned.take(30).trimEnd() + "…" else cleaned
}
