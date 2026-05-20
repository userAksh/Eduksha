package com.dobi.app.feature.parentdashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.ActivityLogRepository
import com.dobi.app.data.repository.ChildEngagementRepository
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ChildEngagement
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.ConceptRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class ParentDashboardUiState(
    val children: List<Child> = emptyList(),
    val engagements: Map<String, ChildEngagement> = emptyMap(),
    val weekPatterns: Map<String, List<DayActivity>> = emptyMap(),
    val insights: Map<String, LearningInsight> = emptyMap(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository,
    private val engagementRepository: ChildEngagementRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val conceptRepository: ConceptRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentDashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadChildren()
        observeEngagements()
        observeActivitySnapshots()
        prefetchCurriculum()
    }

    private fun loadChildren() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }
        childRepository.getChildren(uid)
            .onEach { children ->
                _uiState.value = _uiState.value.copy(
                    children = children,
                    isLoading = false,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Mirrors the same per-child fan-out that ParentHomeViewModel uses: when
     * the children list changes, re-subscribe to one engagement flow per child
     * and combine into a single id → snapshot map. Each engagement flow
     * observes uploads + subjective_results + quiz_attempts for that child.
     */
    private fun observeEngagements() {
        _uiState
            .map { it.children.map(Child::id) }
            .distinctUntilChanged()
            .flatMapLatest { childIds ->
                if (childIds.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    combine(childIds.map { engagementRepository.observeEngagement(it) }) { snaps ->
                        snaps.associateBy { it.childId }
                    }
                }
            }
            .onEach { engagements ->
                _uiState.value = _uiState.value.copy(engagements = engagements)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Keep a curriculum snapshot listener subscribed for each distinct
     * (grade, board) across the parent's children, scoped to the dashboard
     * VM. The data isn't used here — the goal is to warm Firestore's
     * local cache so the Detailed Learning Report's [InsightsViewModel]
     * gets curriculum (and therefore the "Find a topic to assign"
     * recommendation) in a single cache hit instead of a fresh network read.
     */
    private fun prefetchCurriculum() {
        _uiState
            .map { state -> state.children.map { it.grade to it.board }.toSet() }
            .distinctUntilChanged()
            .flatMapLatest { gradeBoards ->
                if (gradeBoards.isEmpty()) {
                    flowOf(Unit)
                } else {
                    combine(
                        gradeBoards.map { (grade, board) ->
                            conceptRepository.getConcepts(grade, board)
                        }
                    ) { _ -> Unit }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * For each child, observe the unified activity log and derive both the
     * Mon→Sun [DayActivity] pattern *and* the top-priority [LearningInsight]
     * from the same item stream — one snapshot listener per source, shared.
     */
    private fun observeActivitySnapshots() {
        _uiState
            .map { it.children.map(Child::id) }
            .distinctUntilChanged()
            .flatMapLatest { childIds ->
                if (childIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        childIds.map { id ->
                            activityLogRepository.observeForChild(id).map { items ->
                                Triple(id, bucketWeek(items), computeLearningInsight(items))
                            }
                        }
                    ) { it.toList() }
                }
            }
            .onEach { snapshots ->
                val patterns = snapshots.associate { it.first to it.second }
                val insights = snapshots.associate { it.first to it.third }
                _uiState.value = _uiState.value.copy(
                    weekPatterns = patterns,
                    insights = insights,
                )
            }
            .launchIn(viewModelScope)
    }
}
