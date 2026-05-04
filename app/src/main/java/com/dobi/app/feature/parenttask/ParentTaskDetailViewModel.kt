package com.dobi.app.feature.parenttask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.domain.repository.ParentTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentTaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ParentTaskRepository
) : ViewModel() {

    private val taskId: String = savedStateHandle.toRoute<Screen.ParentTaskDetail>().taskId

    private val _uiState = MutableStateFlow(ParentTaskDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            repository.getById(taskId).fold(
                onSuccess = { task ->
                    _uiState.update {
                        it.copy(
                            task = task,
                            mcqCount = if (task.isUnitScope) 10 else 5,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't load task")
                    }
                }
            )
        }
    }

    fun onTypeSelected(type: ParentTaskType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun onCustomNoteChanged(note: String) {
        _uiState.update { it.copy(customNote = note) }
    }

    fun onMcqCountChanged(count: Int) {
        _uiState.update { it.copy(mcqCount = count) }
    }

    fun onReviewNoteChanged(note: String) {
        _uiState.update { it.copy(reviewNote = note) }
    }

    fun assignTask() {
        val type = _uiState.value.selectedType ?: return
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            repository.assignByParent(
                taskId = taskId,
                type = type,
                customNote = _uiState.value.customNote.trim(),
                mcqCount = _uiState.value.mcqCount
            ).fold(
                onSuccess = { _uiState.update { it.copy(isSubmitting = false, finished = true) } },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = e.message ?: "Couldn't assign task"
                        )
                    }
                }
            )
        }
    }

    fun acceptSubmission() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            repository.acceptByParent(taskId, _uiState.value.reviewNote.trim()).fold(
                onSuccess = { _uiState.update { it.copy(isSubmitting = false, finished = true) } },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = e.message ?: "Couldn't accept"
                        )
                    }
                }
            )
        }
    }

    fun rejectSubmission() {
        if (_uiState.value.isSubmitting) return
        val note = _uiState.value.reviewNote.trim()
        if (note.isBlank()) {
            _uiState.update {
                it.copy(error = "Please add a note so your child knows what to fix.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            repository.rejectByParent(taskId, note).fold(
                onSuccess = { _uiState.update { it.copy(isSubmitting = false, finished = true) } },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = e.message ?: "Couldn't send"
                        )
                    }
                }
            )
        }
    }
}
