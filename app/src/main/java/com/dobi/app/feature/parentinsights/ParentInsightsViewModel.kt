package com.dobi.app.feature.parentinsights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.ChildInsightsRepository
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ChildInsights
import com.dobi.app.domain.model.InsightsRange
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ParentInsightsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository,
    private val insightsRepository: ChildInsightsRepository,
    private val parentTaskRepository: ParentTaskRepository
) : ViewModel() {

    data class UiState(
        val children: List<Child> = emptyList(),
        val selectedChildId: String? = null,
        val range: InsightsRange = InsightsRange.TODAY,
        val insights: ChildInsights = ChildInsights(),
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val selectedChildId = MutableStateFlow<String?>(null)
    private val selectedRange = MutableStateFlow(InsightsRange.TODAY)

    private val parentTasksFlow: Flow<List<ParentTask>> =
        parentTaskRepository.observeForActiveParent()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadChildren()
        observeCombinedInsights()
    }

    fun onChildSelected(childId: String) {
        selectedChildId.value = childId
    }

    fun onRangeSelected(range: InsightsRange) {
        selectedRange.value = range
    }

    private fun loadChildren() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            childRepository.getChildren(uid).collect { children ->
                val currentSelected = selectedChildId.value
                val resolvedSelected = currentSelected?.takeIf { id ->
                    children.any { it.id == id }
                } ?: children.firstOrNull()?.id
                if (resolvedSelected != currentSelected) {
                    selectedChildId.value = resolvedSelected
                }
                _uiState.value = _uiState.value.copy(
                    children = children,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Single combine driving the insights state — re-runs whenever any of
     * the four inputs changes. The repository emits an insights snapshot
     * without `tasksDone`; we overlay tasks from the parent-task stream so
     * the activity repo doesn't need to know about parent tasks.
     */
    private fun observeCombinedInsights() {
        combine(
            selectedChildId,
            selectedRange,
            parentTasksFlow
        ) { childId, range, tasks -> Triple(childId, range, tasks) }
            .flatMapLatest { (childId, range, tasks) ->
                val insightsSource: Flow<ChildInsights> =
                    if (childId.isNullOrBlank()) flowOf(ChildInsights(range = range))
                    else insightsRepository.observeInsights(childId, range)
                insightsSource.onEach { insights ->
                    val tasksDone = countTasksDone(tasks, childId, range)
                    _uiState.value = _uiState.value.copy(
                        selectedChildId = childId,
                        range = range,
                        insights = insights.copy(tasksDone = tasksDone)
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun countTasksDone(
        tasks: List<ParentTask>,
        childId: String?,
        range: InsightsRange
    ): Int {
        if (childId.isNullOrBlank()) return 0
        val rangeStart = rangeStartMillis(range)
        return tasks.count { t ->
            t.childId == childId &&
                t.status == ParentTaskStatus.ACCEPTED &&
                t.reviewedAtMillis >= rangeStart
        }
    }

    private fun rangeStartMillis(range: InsightsRange): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (range) {
            InsightsRange.TODAY -> Unit
            InsightsRange.WEEK -> cal.add(Calendar.DAY_OF_MONTH, -6)
            InsightsRange.MONTH -> cal.add(Calendar.DAY_OF_MONTH, -29)
        }
        return cal.timeInMillis
    }
}
