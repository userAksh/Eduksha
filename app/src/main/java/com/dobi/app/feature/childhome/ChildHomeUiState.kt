package com.dobi.app.feature.childhome

import com.dobi.app.domain.model.Friend
import com.dobi.app.domain.model.ParentTask

data class ChildHomeUiState(
    val childName: String = "",
    val streakDays: Int = 0,
    val parentTasks: List<ParentTask> = emptyList(),
    val tutorHonorific: String = "",
    val tutorName: String = "",
    val friends: List<Friend> = emptyList(),
    val isLoading: Boolean = false,
    /** Non-blank while a parent-reminder request is in-flight; holds the taskId. */
    val sendingReminderForTaskId: String = "",
    /** Non-blank after a successful reminder send; holds the taskId so the UI
     *  can show a confirmation and reset on the next send. */
    val reminderSentForTaskId: String = "",
)
