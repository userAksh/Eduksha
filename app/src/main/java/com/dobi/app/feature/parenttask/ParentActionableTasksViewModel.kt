package com.dobi.app.feature.parenttask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentActionableTasksUiState(
    val actionableTasks: List<ParentTask> = emptyList(),
    val childById: Map<String, Child> = emptyMap(),
    val sendingReminderForTaskId: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ParentActionableTasksViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val parentTaskRepository: ParentTaskRepository,
    private val childRepository: ChildRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentActionableTasksUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            combine(
                parentTaskRepository.observeForActiveParent(),
                childRepository.getChildren(uid)
            ) { tasks, children ->
                val now = System.currentTimeMillis()
                val twelveH = 12L * 60L * 60L * 1000L
                val actionable = tasks
                    .filter { task ->
                        when (task.status) {
                            ParentTaskStatus.REQUESTED, ParentTaskStatus.PENDING_REVIEW -> true
                            ParentTaskStatus.ASSIGNED ->
                                task.assignedAtMillis > 0L &&
                                    now - task.assignedAtMillis >= twelveH &&
                                    (task.reminderSentAtMillis == 0L ||
                                        now - task.reminderSentAtMillis >= twelveH)
                            else -> false
                        }
                    }
                    .sortedBy { actionableTimestamp(it) }
                _uiState.value = _uiState.value.copy(
                    actionableTasks = actionable,
                    childById = children.associateBy { it.id }
                )
            }.launchIn(viewModelScope)
        }
    }

    fun sendReminder(taskId: String) {
        if (_uiState.value.sendingReminderForTaskId == taskId) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendingReminderForTaskId = taskId)
            parentTaskRepository.sendReminder(taskId)
            _uiState.value = _uiState.value.copy(sendingReminderForTaskId = "")
        }
    }
}

private fun actionableTimestamp(task: ParentTask): Long = when (task.status) {
    ParentTaskStatus.PENDING_REVIEW ->
        if (task.submittedAtMillis > 0L) task.submittedAtMillis else task.assignedAtMillis
    ParentTaskStatus.REQUESTED -> task.requestedAtMillis
    else -> task.assignedAtMillis
}
