package com.dobi.app.feature.conceptdetail

import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.model.Difficulty
import com.dobi.app.domain.model.WorkMode

data class ConceptDetailUiState(
    val concept: Concept? = null,
    val selectedWorkMode: WorkMode? = null,
    val selectedDifficulty: Difficulty? = null,
    val hasActivity: Boolean = false,
    /** Best-guess mode inferred from actual activity (quiz / upload kind). Used
     *  when the student hasn't explicitly picked a Section-1 card. */
    val detectedWorkMode: WorkMode? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isMarkedForRevision: Boolean = false,
    val isTogglingRevision: Boolean = false,
    val toastMessage: String? = null,
    val error: String? = null
) {
    val canMarkAsDone: Boolean
        get() = selectedDifficulty != null && !isSaving
}
