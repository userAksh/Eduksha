package com.dobi.app.feature.homework

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.Homework
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.SaveHomeworkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeworkLogViewModel @Inject constructor(
    private val saveHomeworkUseCase: SaveHomeworkUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeworkLogUiState())
    val uiState = _uiState.asStateFlow()

    fun onSubjectChanged(subject: String) = _uiState.update { it.copy(subject = subject) }
    fun onTeacherNameChanged(name: String) = _uiState.update { it.copy(teacherName = name) }
    fun onDateChanged(millis: Long) = _uiState.update { it.copy(dateMillis = millis) }

    fun onPhotosAdded(uris: List<String>) =
        _uiState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris + uris) }

    fun onPhotoRemoved(uri: String) =
        _uiState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris - uri) }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val childId = userRepository.linkedChildId.first().orEmpty()
            if (childId.isBlank()) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Couldn't find your profile. Please reopen the app."
                    )
                }
                return@launch
            }
            runCatching {
                saveHomeworkUseCase(
                    Homework(
                        childId     = childId,
                        subject     = state.subject,
                        teacherName = state.teacherName,
                        date        = state.isoDate
                    ),
                    state.selectedPhotoUris
                )
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
                return@launch
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
