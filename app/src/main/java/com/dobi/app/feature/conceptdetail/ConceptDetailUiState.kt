package com.dobi.app.feature.conceptdetail

import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.model.Difficulty
import com.dobi.app.domain.model.WorkMode

data class ConceptDetailUiState(
    val concept: Concept? = null,
    val selectedWorkMode: WorkMode? = null,
    val selectedDifficulty: Difficulty? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val canMarkAsDone: Boolean
        get() = selectedWorkMode != null && selectedDifficulty != null && !isSaving
}
