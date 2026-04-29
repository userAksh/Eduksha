package com.dobi.app.feature.uploadwork

import com.dobi.app.domain.model.McqQuestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WorkUploadKind(
    val key: String,
    val title: String,
    val ctaLabel: String,
    val emptyHint: String
) {
    CLASS_WORK(
        key = "class_work",
        title = "Upload Class Work",
        ctaLabel = "Save Class Work",
        emptyHint = "Add photos of the work you did in class"
    ),
    SELF_LEARNING(
        key = "self_learning",
        title = "Upload Self Learning",
        ctaLabel = "Save Self Learning",
        emptyHint = "Add photos of the notes you made on your own"
    ),
    AI_TEST_PREP(
        key = "ai_test_prep",
        title = "AI Practice",
        ctaLabel = "Generate Quiz",
        emptyHint = "Snap pages from the lessons or concepts you picked"
    );

    companion object {
        fun fromKey(key: String): WorkUploadKind =
            values().firstOrNull { it.key == key } ?: CLASS_WORK
    }
}

data class UploadWorkUiState(
    val kind: WorkUploadKind = WorkUploadKind.CLASS_WORK,
    val unitName: String = "",
    val conceptName: String = "",
    val subject: String = "",
    val teacherHonorific: String = "",
    val teacherName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val selectedPhotoUris: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isGeneratingQuiz: Boolean = false,
    val generatedQuiz: List<McqQuestion> = emptyList(),
    val generatedQuizCacheKey: String = "",
    val generatedQuizDisplayName: String = "",
    val error: String? = null
) {
    val canSave: Boolean get() =
        teacherName.isNotBlank() && selectedPhotoUris.isNotEmpty() && !isSaving

    val formattedDate: String get() =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
}
