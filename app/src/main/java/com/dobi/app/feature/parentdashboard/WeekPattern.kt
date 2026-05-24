package com.dobi.app.feature.parentdashboard

import com.dobi.app.data.repository.ActivityLogRepository
import com.dobi.app.data.repository.ActivityLogRepository.ActivityLogItem
import com.dobi.app.data.repository.ActivityLogRepository.Source
import java.util.Calendar

/**
 * Visual kind of a day in the "This Week's Learning Pattern" strip. Derived
 * from whether the child's revision and homework lists are non-empty.
 */
enum class DayKind { REVISION, HOMEWORK, BOTH, NONE }

/**
 * A single learning artifact rendered as a row in the per-day bottom sheet —
 * either a revision (quiz / write-answer / topic upload) or a homework entry.
 * [route] is the existing ActivityLogRepository navigation target so taps
 * land on the right detail screen for the underlying Firestore document.
 */
data class DayItem(
    val title: String,
    val subject: String,
    val route: ActivityLogRepository.DetailRoute,
    val typeTag: String = "",
)

data class DayActivity(
    val revisions: List<DayItem> = emptyList(),
    val homeworks: List<DayItem> = emptyList(),
) {
    val kind: DayKind = when {
        revisions.isEmpty() && homeworks.isEmpty() -> DayKind.NONE
        revisions.isEmpty() -> DayKind.HOMEWORK
        homeworks.isEmpty() -> DayKind.REVISION
        else -> DayKind.BOTH
    }
}

private const val MS_PER_DAY = 24L * 60L * 60L * 1000L

/**
 * Bucket a child's activity log into a 7-element list (Mon → Sun) covering
 * the current local-time week. An item is a revision when it's a quiz
 * attempt, a write-answer session, or an upload of practiced topic / concept
 * work; it's a homework when it's a school-homework entry or a tuition-HW
 * upload (UPLOAD with HOME_WORK category).
 */
fun bucketWeek(
    items: List<ActivityLogItem>,
    now: Long = System.currentTimeMillis(),
): List<DayActivity> {
    val mondayMs = mondayOfThisWeekStart(now)
    val weekEndMs = mondayMs + 7L * MS_PER_DAY
    val revisions = Array(7) { mutableListOf<DayItem>() }
    val homeworks = Array(7) { mutableListOf<DayItem>() }

    for (item in items) {
        if (item.timestamp < mondayMs || item.timestamp >= weekEndMs) continue
        val dayIndex = ((item.timestamp - mondayMs) / MS_PER_DAY).toInt().coerceIn(0, 6)
        val typeTag = when (item.source) {
            Source.QUIZ_MCQ -> "Quiz"
            Source.QUIZ_SUBJECTIVE -> "Write Answer"
            Source.HOMEWORK -> "Homework"
            Source.UPLOAD -> when (item.uploadKind) {
                "class_work" -> "Class Work"
                "self_learning" -> "Self Study"
                "homework" -> "Homework"
                else -> "Upload"
            }
        }
        val dayItem = DayItem(
            title = item.title,
            subject = item.subject,
            route = item.detail,
            typeTag = typeTag,
        )
        if (isHomework(item)) {
            homeworks[dayIndex].add(dayItem)
        } else {
            revisions[dayIndex].add(dayItem)
        }
    }

    return (0 until 7).map { i ->
        DayActivity(revisions = revisions[i].toList(), homeworks = homeworks[i].toList())
    }
}

private fun isHomework(item: ActivityLogItem): Boolean = when (item.source) {
    Source.HOMEWORK -> true
    Source.UPLOAD -> item.category == ActivityLogRepository.Category.HOME_WORK
    else -> false
}

private fun mondayOfThisWeekStart(now: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2 … SATURDAY=7.
    // We want offset to Monday in 0..6 (Mon..Sun).
    val daysToSubtract = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    cal.add(Calendar.DAY_OF_MONTH, -daysToSubtract)
    return cal.timeInMillis
}

val EMPTY_WEEK: List<DayActivity> = List(7) { DayActivity() }
