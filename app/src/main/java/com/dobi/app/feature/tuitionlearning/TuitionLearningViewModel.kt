package com.dobi.app.feature.tuitionlearning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.feature.uploadwork.WorkUploadKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TuitionLearningViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.TuitionLearning>()
    private val kind: WorkUploadKind = WorkUploadKind.fromKey(route.kindKey)

    private val _uiState = MutableStateFlow(
        TuitionLearningUiState(
            kind = kind,
            availableSubjects = subjectsForKind(isHomework = kind.isHomework)
        )
    )
    val uiState = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val honorific = userRepository.tutorHonorific.first()
            val name = userRepository.tutorName.first()
            _uiState.update {
                it.copy(
                    tutorHonorific = honorific,
                    tutorName = name,
                    isLoading = false
                )
            }
        }
    }

    fun onSubjectToggled(subject: TuitionSubject) = _uiState.update { state ->
        val existing = state.entries.firstOrNull { it.subject.name == subject.name }
        val nextEntries = if (existing != null) {
            state.entries - existing
        } else {
            state.entries + TuitionEntry(subject = subject)
        }
        state.copy(entries = nextEntries)
    }

    fun onTopicChanged(subjectName: String, topic: String) = _uiState.update { state ->
        state.copy(entries = state.entries.map {
            if (it.subject.name == subjectName) it.copy(topic = topic.take(80)) else it
        })
    }

    fun onPhotosAdded(subjectName: String, uris: List<String>) = _uiState.update { state ->
        state.copy(entries = state.entries.map {
            if (it.subject.name == subjectName) it.copy(photoUris = it.photoUris + uris) else it
        })
    }

    fun onPhotoRemoved(subjectName: String, uri: String) = _uiState.update { state ->
        state.copy(entries = state.entries.map {
            if (it.subject.name == subjectName) it.copy(photoUris = it.photoUris - uri) else it
        })
    }

    fun onDateChanged(millis: Long) = _uiState.update { it.copy(dateMillis = millis) }

    fun onSave() {
        if (!_uiState.value.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            // TODO: Persist to Firestore so analytics can later group photos
            // by subject + topic. For now we just mark the submission as saved.
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
