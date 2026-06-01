package com.dobi.app.feature.homework

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.ImageScopeRequest
import com.dobi.app.data.repository.ImageScopeValidator
import com.dobi.app.data.repository.UploadEntryRepository
import com.dobi.app.domain.model.Homework
import com.dobi.app.domain.model.UploadEntry
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
    private val userRepository: UserRepository,
    private val uploadEntryRepository: UploadEntryRepository,
    private val imageScopeValidator: ImageScopeValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeworkLogUiState())
    val uiState = _uiState.asStateFlow()

    fun onSubjectChanged(subject: String) = _uiState.update { it.copy(subject = subject) }
    fun onTeacherHonorificChanged(honorific: String) = _uiState.update { it.copy(teacherHonorific = honorific) }
    fun onTeacherNameChanged(name: String) = _uiState.update { it.copy(teacherName = name) }
    fun onChapterNameChanged(name: String) = _uiState.update { it.copy(chapterName = name) }
    fun onDateChanged(millis: Long) = _uiState.update { it.copy(dateMillis = millis) }

    fun onPhotosAdded(uris: List<String>) =
        _uiState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris + uris) }

    fun onPhotoRemoved(uri: String) =
        _uiState.update { it.copy(selectedPhotoUris = it.selectedPhotoUris - uri) }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isValidatingPhotos = true, error = null) }
            val childId = userRepository.linkedChildId.first().orEmpty()
            if (childId.isBlank()) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isValidatingPhotos = false,
                        error = "Couldn't find your profile. Please reopen the app."
                    )
                }
                return@launch
            }
            val grade = userRepository.linkedChildGrade.first()
            val topic = state.chapterName.ifBlank { state.subject }
            val validation = imageScopeValidator.validate(
                ImageScopeRequest(
                    subject = state.subject,
                    grade = grade,
                    topic = topic,
                    lessonContext = state.chapterName.ifBlank { "" },
                    artefactLabel = "homework",
                    photoUris = state.selectedPhotoUris
                )
            )
            if (validation.isFailure) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isValidatingPhotos = false,
                        error = validation.exceptionOrNull()?.message
                            ?: "These photos don't match the subject."
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isValidatingPhotos = false) }

            val fullTeacherName = listOf(state.teacherHonorific, state.teacherName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val homeworkResult = runCatching {
                saveHomeworkUseCase(
                    Homework(
                        childId     = childId,
                        subject     = state.subject,
                        teacherName = fullTeacherName,
                        chapterName = state.chapterName,
                        date        = state.isoDate
                    ),
                    state.selectedPhotoUris
                )
            }
            homeworkResult.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message) }
                return@launch
            }

            // Mirror the upload onto the Revisit Past Topics flow. We reuse
            // the photo URLs already produced by the homework save so we
            // don't pay a second Storage upload.
            val photoUrls = homeworkResult.getOrNull().orEmpty()
            if (photoUrls.isNotEmpty()) {
                val (color, icon) = subjectStyleFor(state.subject)
                uploadEntryRepository.saveWithUrls(
                    entry = UploadEntry(
                        kind = "homework",
                        subject = state.subject,
                        subjectColor = color,
                        subjectIcon = icon,
                        topic = if (state.chapterName.isNotBlank())
                            "${state.chapterName} — Homework"
                        else
                            "Homework — ${state.formattedDate}",
                        timestamp = state.dateMillis
                    ),
                    photoUrls = photoUrls
                ).onFailure { Log.w("HomeworkLog", "Mirror to uploads failed", it) }
            }

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    private fun subjectStyleFor(subject: String): Pair<String, String> = when (subject) {
        "Science" -> "#22C55E" to "🔬"
        "Mathematics" -> "#3B82F6" to "🔢"
        "English" -> "#EC4899" to "📖"
        "Social Science" -> "#F59E0B" to "🌍"
        "Hindi" -> "#EF4444" to "✍️"
        "Sanskrit" -> "#8B5CF6" to "📜"
        else -> "#94A3B8" to "📘"
    }
}
