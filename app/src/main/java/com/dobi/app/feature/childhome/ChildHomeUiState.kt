package com.dobi.app.feature.childhome

import com.dobi.app.domain.model.TodayLearningItem

data class ChildHomeUiState(
    val childName: String = "",
    val streakDays: Int = 3,
    val todayItems: List<TodayLearningItem> = emptyList(),
    val isLoading: Boolean = false
)
