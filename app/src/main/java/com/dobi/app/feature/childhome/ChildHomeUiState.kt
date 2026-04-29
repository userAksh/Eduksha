package com.dobi.app.feature.childhome

import com.dobi.app.domain.model.ParentTask

data class ChildHomeUiState(
    val childName: String = "",
    val streakDays: Int = 3,
    val parentTasks: List<ParentTask> = emptyList(),
    val isLoading: Boolean = false
)
