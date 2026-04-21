package com.dobi.app.feature.homework

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeworkLogUiState(
    val subject: String = "",
    val teacherName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val selectedPhotoUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean get() = subject.isNotBlank() && teacherName.isNotBlank() && !isSaving

    val formattedDate: String get() =
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(dateMillis))

    val isoDate: String get() =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateMillis))
}
