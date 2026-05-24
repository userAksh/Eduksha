package com.dobi.app.feature.revisit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.AttemptScope
import com.dobi.app.data.repository.QuizAttemptRepository
import com.dobi.app.data.repository.SubjectiveResultRepository
import com.dobi.app.data.repository.UploadEntryRepository
import com.dobi.app.domain.model.SubjectiveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A row in the detail screen's attempts list. */
sealed class RevisitAttemptItem {
    abstract val timestamp: Long

    data class McqLast(
        val correct: Int,
        val total: Int,
        val attempts: Int,
        override val timestamp: Long
    ) : RevisitAttemptItem() {
        val accuracyPct: Int get() =
            if (total == 0) 0 else ((correct.toFloat() / total) * 100f).toInt()
    }

    data class Subjective(
        val resultId: String,
        val correct: Int,
        val attempted: Int,
        val accuracyPct: Int,
        override val timestamp: Long
    ) : RevisitAttemptItem()
}

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
    val attempts: List<RevisitAttemptItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val canTakeQuiz: Boolean get() = !isTuition && (conceptId.isNotBlank() || unitId.isNotBlank())
    val hasLastResult: Boolean get() = attempts.any { it is RevisitAttemptItem.McqLast }
    val quizUnitId: String get() = unitId
    val quizConceptId: String get() = conceptId
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RevisitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val uploadEntryRepository: UploadEntryRepository,
    private val quizAttemptRepository: QuizAttemptRepository,
    private val subjectiveResultRepository: SubjectiveResultRepository
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.RevisitDetail>()

    private val _uiState = MutableStateFlow(RevisitDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                uploadEntryRepository.observeForActiveChild(),
                quizAttemptRepository.observeAllSummaries(),
                subjectiveResultRepository.observeForActiveChild()
            ) { entries, summaries, subjectiveResults ->
                Triple(entries, summaries, subjectiveResults)
            }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { (entries, summaries, subjectiveResults) ->
                    val matching = entries.filter { it.groupKey == route.groupKey }
                    if (matching.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false) }
                        return@collect
                    }
                    val latest = matching.maxBy { it.timestamp }
                    val photos = matching
                        .sortedByDescending { it.timestamp }
                        .flatMap { it.photoUrls }

                    val attempts = mutableListOf<RevisitAttemptItem>()
                    val mcqSummary = when {
                        latest.conceptId.isNotBlank() ->
                            summaries[AttemptScope.CONCEPT to latest.conceptId]
                        latest.unitId.isNotBlank() ->
                            summaries[AttemptScope.UNIT to latest.unitId]
                        else -> null
                    }
                    if (mcqSummary != null && mcqSummary.hasResult) {
                        attempts += RevisitAttemptItem.McqLast(
                            correct = mcqSummary.lastCorrect,
                            total = mcqSummary.lastTotal,
                            attempts = mcqSummary.attempts,
                            timestamp = mcqSummary.lastUpdated
                        )
                    }
                    val subjectiveItems = subjectiveResults
                        .filter { it.matchesTopic(latest.unitId, latest.conceptId) }
                        .map { r ->
                            RevisitAttemptItem.Subjective(
                                resultId = r.id,
                                correct = r.correctCount,
                                attempted = r.attemptedCount,
                                accuracyPct = r.accuracyPct,
                                timestamp = r.timestamp
                            )
                        }
                    attempts += subjectiveItems

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
                            attempts = attempts.sortedByDescending { a -> a.timestamp },
                            isLoading = false
                        )
                    }
                }
        }
    }
}

private fun SubjectiveResult.matchesTopic(unitId: String, conceptId: String): Boolean = when {
    conceptId.isNotBlank() -> this.conceptId == conceptId
    unitId.isNotBlank() -> this.unitId == unitId && this.conceptId.isBlank()
    else -> false
}
