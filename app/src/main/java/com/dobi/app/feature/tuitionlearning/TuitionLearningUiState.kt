package com.dobi.app.feature.tuitionlearning

import com.dobi.app.feature.uploadwork.WorkUploadKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TuitionSubject(
    val name: String,
    val color: String,
    val icon: String
)

data class TuitionEntry(
    val subject: TuitionSubject,
    val topic: String = "",
    val photoUris: List<String> = emptyList()
)

data class TuitionLearningUiState(
    val kind: WorkUploadKind = WorkUploadKind.TUITION_WORK_LEARNING,
    val tutorHonorific: String = "",
    val tutorName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val availableSubjects: List<TuitionSubject> = DEFAULT_TUITION_SUBJECTS,
    val entries: List<TuitionEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val tutorDisplay: String get() =
        listOf(tutorHonorific, tutorName).filter { it.isNotBlank() }.joinToString(" ")

    val formattedDate: String get() =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))

    val canSave: Boolean get() =
        tutorName.isNotBlank() &&
            entries.isNotEmpty() &&
            entries.all { it.topic.isNotBlank() && it.photoUris.isNotEmpty() } &&
            !isSaving
}

/**
 * Per-kind copy used inside [TuitionLearningScreen]. Centralised here so the
 * screen stays declarative.
 */
data class TuitionLearningCopy(
    val subtitle: String,
    val topicPlaceholder: String,
    val photoHint: String
) {
    companion object {
        fun forKind(kind: WorkUploadKind): TuitionLearningCopy = when (kind) {
            WorkUploadKind.TUITION_WORK_LEARNING -> TuitionLearningCopy(
                subtitle = "What did you cover in today's tuition class?",
                topicPlaceholder = "e.g. Photosynthesis",
                photoHint = "Snap pages from the notes or lessons your tutor covered."
            )
            WorkUploadKind.TUITION_WORK_WRITTEN -> TuitionLearningCopy(
                subtitle = "What written work did you do in today's tuition?",
                topicPlaceholder = "e.g. Fractions exercise",
                photoHint = "Snap the pages where you wrote or solved questions."
            )
            WorkUploadKind.TUITION_WORK_TEST -> TuitionLearningCopy(
                subtitle = "What test did your tutor give you today?",
                topicPlaceholder = "e.g. Algebra unit test",
                photoHint = "Snap the test paper(s) — questions and your answers."
            )
            WorkUploadKind.TUITION_HW_LEARNING -> TuitionLearningCopy(
                subtitle = "What learning did your tutor assign for home?",
                topicPlaceholder = "e.g. Read about cells",
                photoHint = "Snap the pages or notes you went through at home."
            )
            WorkUploadKind.TUITION_HW_WRITTEN -> TuitionLearningCopy(
                subtitle = "What written homework did your tutor give you?",
                topicPlaceholder = "e.g. Linear equations Q1–Q10",
                photoHint = "Snap the homework you finished at home."
            )
            else -> TuitionLearningCopy(
                subtitle = "Tell us what you covered today.",
                topicPlaceholder = "Topic",
                photoHint = "Add photos of your work."
            )
        }
    }
}

/** Vibrant palette used when the child is logging tuition class work. */
val DEFAULT_TUITION_SUBJECTS: List<TuitionSubject> = listOf(
    TuitionSubject("Science", "#22C55E", "🔬"),
    TuitionSubject("Mathematics", "#3B82F6", "🔢"),
    TuitionSubject("English", "#EC4899", "📖"),
    TuitionSubject("Social Science", "#F59E0B", "🌍"),
    TuitionSubject("Hindi", "#EF4444", "✍️"),
    TuitionSubject("Sanskrit", "#8B5CF6", "📜")
)

/** Cooler / warmer alternate palette to visually distinguish homework uploads. */
val HOMEWORK_TUITION_SUBJECTS: List<TuitionSubject> = listOf(
    TuitionSubject("Science", "#0EA5E9", "🔬"),
    TuitionSubject("Mathematics", "#6366F1", "🔢"),
    TuitionSubject("English", "#D946EF", "📖"),
    TuitionSubject("Social Science", "#F97316", "🌍"),
    TuitionSubject("Hindi", "#DC2626", "✍️"),
    TuitionSubject("Sanskrit", "#7C3AED", "📜")
)

fun subjectsForKind(isHomework: Boolean): List<TuitionSubject> =
    if (isHomework) HOMEWORK_TUITION_SUBJECTS else DEFAULT_TUITION_SUBJECTS
