package com.dobi.app.feature.mylearnings

data class MyLearningsUiState(
    val todaysItems: List<TodayLearningEntry> = emptyList(),
    val savedItems: List<SavedItem> = emptyList(),
    val currentAffairs: List<CurrentAffair> = emptyList(),
    val isLoading: Boolean = false
)

enum class TodayKind { HOMEWORK, TASK, QUIZ }

data class TodayLearningEntry(
    val id: String,
    val kind: TodayKind,
    val title: String,
    val subject: String,
    val timeLabel: String,
    val scoreLabel: String? = null
)

enum class SavedKind { LESSON, CONCEPT }

data class SavedItem(
    val id: String,
    val kind: SavedKind,
    val title: String,
    val subject: String,
    val subjectEmoji: String,
    val conceptCount: Int? = null
)

data class CurrentAffair(
    val id: String,
    val headline: String,
    val category: String,
    val timeAgo: String,
    val emoji: String
)
