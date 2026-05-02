package com.dobi.app.feature.childhome

import com.dobi.app.domain.model.ParentTask

data class ChildHomeUiState(
    val childName: String = "",
    val streakDays: Int = 0,
    val parentTasks: List<ParentTask> = emptyList(),
    val tutorHonorific: String = "",
    val tutorName: String = "",
    val isLoading: Boolean = false
)
