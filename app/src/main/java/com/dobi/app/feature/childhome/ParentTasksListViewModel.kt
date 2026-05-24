package com.dobi.app.feature.childhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.repository.ParentTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentTasksListViewModel @Inject constructor(
    private val parentTaskRepository: ParentTaskRepository,
) : ViewModel() {

    data class UiState(
        val activeTasks: List<ParentTask> = emptyList(),
        val requestedTasks: List<ParentTask> = emptyList(),
        val sendingReminderForTaskId: String = "",
        val reminderSentForTaskId: String = "",
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        parentTaskRepository.observeForActiveChild()
            .catch { /* leave lists empty on error */ }
            .onEach { tasks ->
                _uiState.value = _uiState.value.copy(
                    activeTasks = tasks.filter { it.status != ParentTaskStatus.REQUESTED },
                    requestedTasks = tasks.filter { it.status == ParentTaskStatus.REQUESTED },
                )
            }
            .launchIn(viewModelScope)
    }

    fun sendReminderToParent(taskId: String) {
        if (_uiState.value.sendingReminderForTaskId == taskId) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendingReminderForTaskId = taskId)
            parentTaskRepository.sendReminderToParent(taskId)
            _uiState.value = _uiState.value.copy(
                sendingReminderForTaskId = "",
                reminderSentForTaskId = taskId,
            )
        }
    }
}
