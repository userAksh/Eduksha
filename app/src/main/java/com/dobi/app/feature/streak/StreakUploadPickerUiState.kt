package com.dobi.app.feature.streak

import com.dobi.app.feature.conceptlist.SubjectGroup

sealed interface StreakPickerSelection {
    data class Lesson(val unitId: String) : StreakPickerSelection
    data class ConceptPick(val conceptId: String, val unitId: String) : StreakPickerSelection
}

data class StreakUploadPickerUiState(
    val subjectGroups: List<SubjectGroup> = emptyList(),
    val searchQuery: String = "",
    val selection: StreakPickerSelection? = null,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val canContinue: Boolean get() = selection != null
}
