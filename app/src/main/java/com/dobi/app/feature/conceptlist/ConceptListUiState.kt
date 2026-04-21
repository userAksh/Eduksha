package com.dobi.app.feature.conceptlist

import com.dobi.app.domain.model.Concept

data class ConceptListUiState(
    val subjectGroups: List<SubjectGroup> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class SubjectGroup(
    val subject: String,
    val subjectColor: String,
    val subjectIcon: String,
    val units: List<UnitGroup>,
    val isExpanded: Boolean = false
)

data class UnitGroup(
    val unitId: String,
    val unitName: String,
    val concepts: List<Concept>,
    val isExpanded: Boolean = false
)
