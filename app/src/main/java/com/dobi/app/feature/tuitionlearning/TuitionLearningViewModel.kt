package com.dobi.app.feature.tuitionlearning

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.ImageScopeRequest
import com.dobi.app.data.repository.ImageScopeValidator
import com.dobi.app.data.repository.UploadEntryRepository
import com.dobi.app.domain.model.UploadEntry
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
    private val userRepository: UserRepository,
    private val uploadEntryRepository: UploadEntryRepository,
    private val imageScopeValidator: ImageScopeValidator
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
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isValidatingPhotos = true, error = null) }
            val grade = userRepository.linkedChildGrade.first()
            val artefact = if (state.kind.isHomework) "tuition homework" else "tuition class work"

            // Validate every per-subject batch up front so the child gets a
            // single consolidated error and we don't leave a partial save behind.
            val rejected = state.entries.firstNotNullOfOrNull { entry ->
                val res = imageScopeValidator.validate(
                    ImageScopeRequest(
                        subject = entry.subject.name,
                        grade = grade,
                        topic = entry.topic,
                        artefactLabel = artefact,
                        photoUris = entry.photoUris
                    )
                )
                res.exceptionOrNull()?.let { entry to it }
            }
            if (rejected != null) {
                val (entry, err) = rejected
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isValidatingPhotos = false,
                        error = "${entry.subject.name}: ${err.message ?: "Photos don't match the topic."}"
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isValidatingPhotos = false) }

            val failures = mutableListOf<Throwable>()
            state.entries.forEach { entry ->
                val upload = UploadEntry(
                    kind = state.kind.key,
                    subject = entry.subject.name,
                    subjectColor = entry.subject.color,
                    subjectIcon = entry.subject.icon,
                    topic = entry.topic,
                    timestamp = state.dateMillis
                )
                uploadEntryRepository.save(upload, entry.photoUris)
                    .onFailure { failures += it }
            }
            if (failures.isEmpty()) {
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } else {
                Log.w("TuitionLearning", "Saved with ${failures.size} failure(s)", failures.first())
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Saved some uploads but a few photos didn't go through. Please retry."
                    )
                }
            }
        }
    }
}
