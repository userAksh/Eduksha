package com.dobi.app.feature.classtestprep

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TestPrepSubject(
    val name: String,
    val color: String,
    val icon: String
)

data class TestPrepConcept(
    val id: String,
    val name: String
)

data class TestPrepUnit(
    val unitId: String,
    val unitName: String,
    val concepts: List<TestPrepConcept>
)

data class TestPrepSelection(
    val id: String,
    val title: String,
    val isLesson: Boolean,
    val subject: String,
    val unitId: String,
    val unitName: String
)

data class ClassTestPrepUiState(
    val subjects: List<TestPrepSubject> = emptyList(),
    val selectedSubject: String? = null,
    val teacherHonorific: String = "",
    val teacherName: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val units: List<TestPrepUnit> = emptyList(),
    val selectedItems: List<TestPrepSelection> = emptyList(),
    val showLimitInfo: Boolean = false,
    val limitMessage: String? = null,
    val showModePopup: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val canStart: Boolean get() =
        teacherName.isNotBlank() && selectedItems.isNotEmpty()

    val formattedDate: String get() =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))

    companion object {
        const val MAX_SELECTIONS = 2
    }
}
