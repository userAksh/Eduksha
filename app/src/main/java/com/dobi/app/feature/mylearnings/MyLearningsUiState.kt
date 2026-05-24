package com.dobi.app.feature.mylearnings

import com.dobi.app.domain.model.RevisionMark

data class MyLearningsUiState(
    val todaysItems: List<TodayLearningEntry> = emptyList(),
    val revisionMarks: List<RevisionMark> = emptyList(),
    val currentAffairs: List<CurrentAffair> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingNews: Boolean = false,
    val newsError: String? = null,
    val selectedStoryIndex: Int? = null
)

enum class TodayKind { HOMEWORK, TASK, QUIZ }

enum class TodayStatus { COMPLETED, PENDING }

/**
 * Where a Today's Learning card should navigate when tapped. ChildHome
 * dispatches each variant to the right NavGraph route.
 */
sealed interface TodayNavTarget {
    /** Re-open a curated MCQ in result-view mode. */
    data class QuizResult(val unitId: String, val conceptId: String) : TodayNavTarget
    /** Open a stored written-answer (subjective) result. */
    data class Subjective(val resultId: String) : TodayNavTarget
    /** Open the parent-task detail / submission view for the child. */
    data class ParentTask(val taskId: String) : TodayNavTarget
    /** Open the Revisit-Past-Topics group for an upload (homework/self/tuition). */
    data class UploadGroup(val groupKey: String) : TodayNavTarget
}

data class TodayLearningEntry(
    val id: String,
    val kind: TodayKind,
    val title: String,
    val subject: String,
    val timeLabel: String,
    val status: TodayStatus = TodayStatus.COMPLETED,
    val scoreLabel: String? = null,
    val timestamp: Long = 0L,
    val navTarget: TodayNavTarget? = null,
    val assignedByParent: Boolean = false
)

data class CurrentAffair(
    val id: String,
    val headline: String,
    val category: String,
    val timeAgo: String,
    val emoji: String,
    val body: String = ""
)
