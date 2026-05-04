package com.dobi.app.feature.parenttask

import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskType

data class ParentTaskDetailUiState(
    val task: ParentTask? = null,
    // Assign-form local state
    val selectedType: ParentTaskType? = null,
    val customNote: String = "",
    val mcqCount: Int = 5,
    // Review-form local state
    val reviewNote: String = "",
    // Async flags
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val finished: Boolean = false,
    val error: String? = null
) {
    val mcqDefault: Int
        get() = if (task?.isUnitScope == true) 10 else 5
}
