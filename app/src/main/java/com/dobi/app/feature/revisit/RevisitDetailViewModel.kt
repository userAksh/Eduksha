package com.dobi.app.feature.revisit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.UploadEntryRepository
import com.dobi.app.domain.model.UploadEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RevisitDetailUiState(
    val title: String = "",
    val subject: String = "",
    val subjectColor: String = "",
    val subjectIcon: String = "",
    val photoUrls: List<String> = emptyList(),
    val unitId: String = "",
    val conceptId: String = "",
    val isTuition: Boolean = false,
    val latestTimestamp: Long = 0L,
    val uploadCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val canTakeQuiz: Boolean get() = !isTuition && (conceptId.isNotBlank() || unitId.isNotBlank())
    val quizUnitId: String get() = unitId
    val quizConceptId: String get() = conceptId
}

@HiltViewModel
class RevisitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val uploadEntryRepository: UploadEntryRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.RevisitDetail>()

    private val _uiState = MutableStateFlow(RevisitDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            uploadEntryRepository.observeForActiveChild()
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { all ->
                    val matching = all.filter { it.groupKey == route.groupKey }
                    if (matching.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false) }
                        return@collect
                    }
                    val latest = matching.maxBy { it.timestamp }
                    val photos = matching
                        .sortedByDescending { it.timestamp }
                        .flatMap { it.photoUrls }
                    _uiState.update {
                        it.copy(
                            title = latest.displayTitle,
                            subject = latest.subject,
                            subjectColor = latest.subjectColor,
                            subjectIcon = latest.subjectIcon,
                            photoUrls = photos,
                            unitId = latest.unitId,
                            conceptId = latest.conceptId,
                            isTuition = latest.isTuition,
                            latestTimestamp = latest.timestamp,
                            uploadCount = matching.size,
                            isLoading = false
                        )
                    }
                }
        }
    }
}
