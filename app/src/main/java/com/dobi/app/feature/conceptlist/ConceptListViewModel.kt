package com.dobi.app.feature.conceptlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConceptListViewModel @Inject constructor(
    private val getConceptsUseCase: GetConceptsUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConceptListUiState())
    val uiState = _uiState.asStateFlow()

    // canonical grouped data, holds expand state
    private var canonicalGroups: List<SubjectGroup> = emptyList()

    init {
        loadConcepts()
    }

    private fun loadConcepts() {
        viewModelScope.launch {
            userRepository.linkedChildGrade
                .flatMapLatest { grade -> getConceptsUseCase(grade = grade, board = "CBSE") }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { concepts ->
                    val groups = buildGroups(concepts)
                    canonicalGroups = groups
                    _uiState.update { state ->
                        state.copy(
                            subjectGroups = filterGroups(groups, state.searchQuery),
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, subjectGroups = filterGroups(canonicalGroups, query)) }
    }

    fun onSubjectToggled(subject: String) {
        canonicalGroups = canonicalGroups.map { group ->
            if (group.subject == subject) group.copy(isExpanded = !group.isExpanded) else group
        }
        _uiState.update { state ->
            state.copy(subjectGroups = filterGroups(canonicalGroups, state.searchQuery))
        }
    }

    fun onUnitToggled(subject: String, unitId: String) {
        canonicalGroups = canonicalGroups.map { group ->
            if (group.subject != subject) group
            else group.copy(
                units = group.units.map { unit ->
                    if (unit.unitId == unitId) unit.copy(isExpanded = !unit.isExpanded) else unit
                }
            )
        }
        _uiState.update { state ->
            state.copy(subjectGroups = filterGroups(canonicalGroups, state.searchQuery))
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun buildGroups(concepts: List<Concept>): List<SubjectGroup> =
        concepts
            .groupBy { it.subject }
            .entries
            .sortedBy { it.key }
            .mapIndexed { index, (subject, subjectConcepts) ->
                val first = subjectConcepts.first()
                SubjectGroup(
                    subject = subject,
                    subjectColor = first.subjectColor,
                    subjectIcon = first.subjectIcon,
                    isExpanded = index == 0, // first subject expanded by default
                    units = subjectConcepts
                        .groupBy { it.unitId }
                        .entries
                        .sortedBy { it.value.first().unitOrder }
                        .map { (unitId, unitConcepts) ->
                            UnitGroup(
                                unitId = unitId,
                                unitName = unitConcepts.first().unit,
                                concepts = unitConcepts.sortedBy { it.order },
                                isExpanded = false
                            )
                        }
                )
            }

    private fun filterGroups(groups: List<SubjectGroup>, query: String): List<SubjectGroup> {
        if (query.isBlank()) return groups
        val q = query.lowercase().trim()
        return groups.mapNotNull { group ->
            val matchingUnits = group.units.mapNotNull { unit ->
                val matchingConcepts = unit.concepts.filter { concept ->
                    concept.name.lowercase().contains(q) ||
                    concept.aliases.any { it.lowercase().contains(q) } ||
                    unit.unitName.lowercase().contains(q) ||
                    group.subject.lowercase().contains(q)
                }
                if (matchingConcepts.isEmpty()) null
                else unit.copy(concepts = matchingConcepts, isExpanded = true)
            }
            if (matchingUnits.isEmpty()) null
            else group.copy(units = matchingUnits, isExpanded = true)
        }
    }
}
