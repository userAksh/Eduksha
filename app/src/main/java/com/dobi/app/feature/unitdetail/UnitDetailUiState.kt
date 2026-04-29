package com.dobi.app.feature.unitdetail

import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.model.Difficulty
import com.dobi.app.domain.model.WorkMode

data class UnitDetailUiState(
    val unitName: String = "",
    val subject: String = "",
    val subjectColor: String = "#22C55E",
    val subjectIcon: String = "🔬",
    val concepts: List<Concept> = emptyList(),
    val coveredConceptIds: Set<String> = emptySet(),
    val selectedWorkMode: WorkMode? = null,
    val selectedDifficulty: Difficulty? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val canMarkAsDone: Boolean
        get() = selectedWorkMode != null &&
            selectedDifficulty != null &&
            !isSaving &&
            coveredConceptIds.isNotEmpty()
}
