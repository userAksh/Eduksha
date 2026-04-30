package com.dobi.app.feature.uploadwork

import com.dobi.app.domain.model.McqQuestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WorkUploadKind(
    val key: String,
    val title: String,
    val ctaLabel: String,
    val emptyHint: String,
    val isTuition: Boolean = false,
    val isHomework: Boolean = false
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
    ),
    TUITION_WORK_LEARNING(
        key = "tuition_work_learning",
        title = "Today's Learning",
        ctaLabel = "Save Today's Learning",
        emptyHint = "Add photos of what you learned in tuition today",
        isTuition = true
    ),
    TUITION_WORK_WRITTEN(
        key = "tuition_work_written",
        title = "Written Work",
        ctaLabel = "Save Written Work",
        emptyHint = "Add photos of the written work you did in tuition",
        isTuition = true
    ),
    TUITION_WORK_TEST(
        key = "tuition_work_test",
        title = "Today's Test",
        ctaLabel = "Save Today's Test",
        emptyHint = "Add photos of the test your tutor gave today",
        isTuition = true
    ),
    TUITION_HW_LEARNING(
        key = "tuition_hw_learning",
        title = "Homework — Today's Learning",
        ctaLabel = "Save Homework Learning",
        emptyHint = "Add photos of the reading or learning your tutor assigned",
        isTuition = true,
        isHomework = true
    ),
    TUITION_HW_WRITTEN(
        key = "tuition_hw_written",
        title = "Homework — Written Work",
        ctaLabel = "Save Homework",
        emptyHint = "Add photos of the homework written by you",
        isTuition = true,
        isHomework = true
    );

    companion object {
        fun fromKey(key: String): WorkUploadKind =
            values().firstOrNull { it.key == key } ?: CLASS_WORK
    }
}

data class UploadWorkUiState(
    val kind: WorkUploadKind = WorkUploadKind.CLASS_WORK,
    val unitId: String = "",
    val unitName: String = "",
    val conceptId: String = "",
    val conceptName: String = "",
    val subject: String = "",
    val subjectColor: String = "",
    val subjectIcon: String = "",
    val teacherHonorific: String = "",
    val teacherName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val selectedPhotoUris: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isValidatingPhotos: Boolean = false,
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
