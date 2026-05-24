package com.dobi.app.feature.earnreward

import com.dobi.app.domain.model.ParentTask
import com.dobi.app.feature.conceptlist.SubjectGroup

sealed interface EarnRewardSelection {
    data class Lesson(val unitId: String) : EarnRewardSelection
    data class ConceptPick(val conceptId: String, val unitId: String) : EarnRewardSelection
}

data class EarnRewardPickerUiState(
    val subjectGroups: List<SubjectGroup> = emptyList(),
    val searchQuery: String = "",
    val selection: EarnRewardSelection? = null,
    /** Non-null when a REQUESTED task was sent < 12 hours ago and parent hasn't responded. */
    val pendingRequest: ParentTask? = null,
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val sentSuccessfully: Boolean = false,
    val error: String? = null
) {
    val canContinue: Boolean get() = selection != null && !isSending && pendingRequest == null
}

data class ResolvedEarnRewardSelection(
    val isUnitScope: Boolean,
    val unitId: String,
    val conceptId: String,
    val displayName: String,
    val subject: String
)
