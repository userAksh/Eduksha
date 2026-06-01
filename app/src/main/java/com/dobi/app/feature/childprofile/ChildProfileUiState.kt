package com.dobi.app.feature.childprofile

import com.dobi.app.domain.model.Friend

data class ChildProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val grade: Int = 0,
    val board: String = "CBSE",
    val gender: String = "boy",
    val streakDays: Int = 0,
    val topicsCount: Int = 0,
    val quizzesCount: Int = 0,
    val averageAccuracyPct: Int = 0,
    val subjects: List<SubjectStat> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val deleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false
)

data class SubjectStat(
    val name: String,
    val emoji: String,
    val topicsCount: Int,
    val accuracyPct: Int?,
    val topics: List<TopicActivityItem> = emptyList()
)

data class TopicActivityItem(
    val name: String,
    val groupKey: String,
    val quizUnitId: String,
    val quizConceptId: String,
    val uploadKinds: Set<String>,
    val hasQuiz: Boolean,
    val hasSubjective: Boolean,
    val accuracyPct: Int?,
    val lastActivityMs: Long
)
