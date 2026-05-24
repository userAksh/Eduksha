package com.dobi.app.feature.earnreward

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.repository.ParentTaskRepository
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import com.dobi.app.feature.conceptlist.SubjectGroup
import com.dobi.app.feature.conceptlist.UnitGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EarnRewardPickerViewModel @Inject constructor(
    private val getConceptsUseCase: GetConceptsUseCase,
    private val userRepository: UserRepository,
    private val parentTaskRepository: ParentTaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarnRewardPickerUiState())
    val uiState = _uiState.asStateFlow()

    private var canonicalGroups: List<SubjectGroup> = emptyList()
    private var allConcepts: List<Concept> = emptyList()

    init {
        load()
        observePendingRequest()
    }

    private fun observePendingRequest() {
        val twelveHoursMs = 12 * 60 * 60 * 1000L
        parentTaskRepository.observeForActiveChild()
            .catch { }
            .onEach { tasks ->
                val pending = tasks
                    .filter { it.status == ParentTaskStatus.REQUESTED }
                    .filter { System.currentTimeMillis() - it.requestedAtMillis < twelveHoursMs }
                    .maxByOrNull { it.requestedAtMillis }
                _uiState.update { it.copy(pendingRequest = pending) }
            }
            .launchIn(viewModelScope)
    }

    private fun load() {
        viewModelScope.launch {
            userRepository.linkedChildGrade
                .flatMapLatest { grade -> getConceptsUseCase(grade = grade, board = "CBSE") }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { concepts ->
                    allConcepts = concepts
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
        _uiState.update {
            it.copy(searchQuery = query, subjectGroups = filterGroups(canonicalGroups, query))
        }
    }

    fun onSubjectToggled(subject: String) {
        canonicalGroups = canonicalGroups.map { group ->
            if (group.subject == subject) group.copy(isExpanded = !group.isExpanded) else group
        }
        _uiState.update { state ->
            state.copy(subjectGroups = filterGroups(canonicalGroups, state.searchQuery))
        }
    }

    fun onLessonSelected(unitId: String) {
        _uiState.update { state ->
            val current = state.selection
            val nextSelection: EarnRewardSelection? =
                if (current is EarnRewardSelection.Lesson && current.unitId == unitId) null
                else EarnRewardSelection.Lesson(unitId)
            state.copy(selection = nextSelection)
        }
    }

    fun onConceptSelected(conceptId: String, unitId: String) {
        _uiState.update { state ->
            val current = state.selection
            val nextSelection: EarnRewardSelection? =
                if (current is EarnRewardSelection.ConceptPick && current.conceptId == conceptId) null
                else EarnRewardSelection.ConceptPick(conceptId, unitId)
            state.copy(selection = nextSelection)
        }
    }

    /**
     * Resolve the chosen selection into the data the parent needs to see
     * when assigning a task. Once Phase 1 (Firestore) lands, this gets passed
     * into the request-write call.
     */
    fun resolveSelection(): ResolvedEarnRewardSelection? {
        val sel = _uiState.value.selection ?: return null
        return when (sel) {
            is EarnRewardSelection.Lesson -> {
                val first = allConcepts.firstOrNull { it.unitId == sel.unitId } ?: return null
                ResolvedEarnRewardSelection(
                    isUnitScope = true,
                    unitId = sel.unitId,
                    conceptId = "",
                    displayName = first.unit,
                    subject = first.subject
                )
            }
            is EarnRewardSelection.ConceptPick -> {
                val concept = allConcepts.firstOrNull { it.id == sel.conceptId } ?: return null
                ResolvedEarnRewardSelection(
                    isUnitScope = false,
                    unitId = concept.unitId,
                    conceptId = concept.id,
                    displayName = concept.name,
                    subject = concept.subject
                )
            }
        }
    }

    fun sendRequestToParent() {
        val resolved = resolveSelection() ?: return
        if (_uiState.value.isSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            val concept = if (resolved.isUnitScope) null
            else allConcepts.firstOrNull { it.id == resolved.conceptId }
            val firstInUnit = allConcepts.firstOrNull { it.unitId == resolved.unitId }
            val subjectColor = (concept ?: firstInUnit)?.subjectColor.orEmpty()
            val subjectIcon = (concept ?: firstInUnit)?.subjectIcon.orEmpty()
            val unitName = firstInUnit?.unit.orEmpty()
            parentTaskRepository.requestFromChild(
                unitId = resolved.unitId,
                unitName = unitName,
                conceptId = resolved.conceptId,
                conceptName = if (resolved.isUnitScope) "" else resolved.displayName,
                isUnitScope = resolved.isUnitScope,
                subject = resolved.subject,
                subjectColor = subjectColor,
                subjectIcon = subjectIcon
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isSending = false, sentSuccessfully = true)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = e.message ?: "Couldn't send request. Try again."
                        )
                    }
                }
            )
        }
    }

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
                    isExpanded = index == 0,
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
