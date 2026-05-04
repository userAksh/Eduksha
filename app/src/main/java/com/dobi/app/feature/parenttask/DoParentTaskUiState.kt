package com.dobi.app.feature.parenttask

import com.dobi.app.domain.model.ParentTask

data class DoParentTaskUiState(
    val task: ParentTask? = null,
    val isLoading: Boolean = true,
    val selectedPhotoUris: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val isPreparingQuiz: Boolean = false,
    val launchQuizCacheKey: String? = null,
    val finished: Boolean = false,
    val error: String? = null
) {
    val canSubmitPhotos: Boolean get() = selectedPhotoUris.isNotEmpty() && !isSubmitting
}
