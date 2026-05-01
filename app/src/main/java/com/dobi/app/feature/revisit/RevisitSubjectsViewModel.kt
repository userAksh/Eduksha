package com.dobi.app.feature.revisit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.AttemptScope
import com.dobi.app.data.repository.QuizAttemptRepository
import com.dobi.app.data.repository.SubjectiveResultRepository
import com.dobi.app.data.repository.UploadEntryRepository
import com.dobi.app.domain.model.SubjectiveResult
import com.dobi.app.domain.model.UploadEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A subject pill in the horizontal filter row. */
data class RevisitSubjectChip(
    val name: String,
    val color: String,
    val icon: String,
    val topicCount: Int
)

/** A single card in the topic list. */
data class RevisitTopicCard(
    val groupKey: String,
    val title: String,
    val subject: String,
    val subjectColor: String,
    val subjectIcon: String,
    val context: String,
    val photoCount: Int,
    val latestTimestamp: Long,
    val kindLabel: String,
    val kindColor: String,
    val unitId: String = "",
    val conceptId: String = "",
    /** Last MCQ score % if any attempts exist on this topic. */
    val mcqLastPct: Int? = null,
    val mcqAttempts: Int = 0,
    /** Latest subjective accuracy % across all sessions on this topic. */
    val subjectiveLastPct: Int? = null,
    val subjectiveAttempts: Int = 0
)

data class RevisitSubjectsUiState(
    val searchQuery: String = "",
    val selectedSubject: String = "All",
    val subjectChips: List<RevisitSubjectChip> = emptyList(),
    val visibleTopics: List<RevisitTopicCard> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalTopicCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RevisitSubjectsViewModel @Inject constructor(
    private val uploadEntryRepository: UploadEntryRepository,
    private val quizAttemptRepository: QuizAttemptRepository,
    private val subjectiveResultRepository: SubjectiveResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RevisitSubjectsUiState())
    val uiState = _uiState.asStateFlow()

    private var allTopics: List<RevisitTopicCard> = emptyList()

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
                    // Tuition uploads live in their own world (parent-facing
                    // tutoring log) — they shouldn't surface in Self Revision's
                    // Revisit Past Topics, which is for the child's own school
                    // work and homework.
                    val schoolEntries = entries.filterNot { it.isTuition }
                    allTopics = buildTopics(schoolEntries, summaries, subjectiveResults)
                    val chips = buildChips(schoolEntries)
                    _uiState.update {
                        it.copy(
                            subjectChips = chips,
                            visibleTopics = filter(allTopics, it.selectedSubject, it.searchQuery),
                            isLoading = false,
                            totalTopicCount = allTopics.size
                        )
                    }
                }
        }
    }

    fun onSubjectSelected(subjectName: String) = _uiState.update {
        it.copy(
            selectedSubject = subjectName,
            visibleTopics = filter(allTopics, subjectName, it.searchQuery)
        )
    }

    fun onSearchQueryChanged(query: String) = _uiState.update {
        it.copy(
            searchQuery = query,
            visibleTopics = filter(allTopics, it.selectedSubject, query)
        )
    }

    private fun buildChips(entries: List<UploadEntry>): List<RevisitSubjectChip> {
        val grouped = entries
            .filter { it.subject.isNotBlank() }
            .groupBy { it.subject }
        val perSubject = grouped.map { (subject, items) ->
            val first = items.first()
            RevisitSubjectChip(
                name = subject,
                color = first.subjectColor,
                icon = first.subjectIcon,
                topicCount = items.map { it.groupKey }.distinct().size
            )
        }.sortedBy { it.name }
        val all = RevisitSubjectChip(
            name = "All",
            color = "",
            icon = "",
            topicCount = perSubject.sumOf { it.topicCount }
        )
        return listOf(all) + perSubject
    }

    private fun buildTopics(
        entries: List<UploadEntry>,
        quizSummaries: Map<Pair<AttemptScope, String>, com.dobi.app.data.repository.QuizAttemptRepository.QuizAttemptSummary>,
        subjectiveResults: List<SubjectiveResult>
    ): List<RevisitTopicCard> {
        val subjectiveByConcept = subjectiveResults
            .filter { it.conceptId.isNotBlank() }
            .groupBy { it.conceptId }
        val subjectiveByUnit = subjectiveResults
            .filter { it.conceptId.isBlank() && it.unitId.isNotBlank() }
            .groupBy { it.unitId }

        return entries.groupBy { it.groupKey }
            .map { (key, items) ->
                val latest = items.maxBy { it.timestamp }
                val (label, color) = kindStyle(latest.kind)

                val mcqSummary = when {
                    latest.conceptId.isNotBlank() ->
                        quizSummaries[AttemptScope.CONCEPT to latest.conceptId]
                    latest.unitId.isNotBlank() ->
                        quizSummaries[AttemptScope.UNIT to latest.unitId]
                    else -> null
                }
                val subjectiveList = when {
                    latest.conceptId.isNotBlank() -> subjectiveByConcept[latest.conceptId].orEmpty()
                    latest.unitId.isNotBlank() -> subjectiveByUnit[latest.unitId].orEmpty()
                    else -> emptyList()
                }
                val latestSubjective = subjectiveList.maxByOrNull { it.timestamp }

                RevisitTopicCard(
                    groupKey = key,
                    title = latest.displayTitle,
                    subject = latest.subject,
                    subjectColor = latest.subjectColor,
                    subjectIcon = latest.subjectIcon,
                    context = contextLine(latest),
                    photoCount = items.sumOf { it.photoUrls.size },
                    latestTimestamp = latest.timestamp,
                    kindLabel = label,
                    kindColor = color,
                    unitId = latest.unitId,
                    conceptId = latest.conceptId,
                    mcqLastPct = mcqSummary?.takeIf { it.hasResult }?.accuracyPct,
                    mcqAttempts = mcqSummary?.attempts ?: 0,
                    subjectiveLastPct = latestSubjective?.accuracyPct,
                    subjectiveAttempts = subjectiveList.size
                )
            }
            .sortedByDescending { it.latestTimestamp }
    }

    private fun kindStyle(kind: String): Pair<String, String> = when (kind) {
        "class_work" -> "Class Work" to "#3B82F6"
        "self_learning" -> "Self Learning" to "#8B5CF6"
        "ai_test_prep" -> "AI Practice" to "#EC4899"
        "homework" -> "Homework" to "#F97316"
        "tuition_work_learning" -> "Tuition Class" to "#22C55E"
        "tuition_work_written" -> "Tuition Written" to "#0EA5E9"
        "tuition_work_test" -> "Tuition Test" to "#EF4444"
        "tuition_hw_learning" -> "Tuition HW" to "#F59E0B"
        "tuition_hw_written" -> "Tuition HW" to "#F59E0B"
        else -> "Upload" to "#94A3B8"
    }

    /**
     * Secondary line shown under the topic title. Avoids repeating what's
     * already in the title — e.g. when the title is the unit name we show
     * only the subject, not "Subject • <same unit name>".
     */
    private fun contextLine(e: UploadEntry): String = when {
        e.kind == "homework" -> e.subject.ifBlank { "Homework" }
        e.isTuition -> e.subject.ifBlank { "Tuition" }
        e.conceptId.isNotBlank() && e.unitName.isNotBlank() ->
            "${e.subject} • ${e.unitName}"
        e.unitName.isNotBlank() -> e.subject
        else -> e.subject.ifBlank { "" }
    }

    private fun filter(
        all: List<RevisitTopicCard>,
        selectedSubject: String,
        query: String
    ): List<RevisitTopicCard> {
        val bySubject = if (selectedSubject == "All" || selectedSubject.isBlank()) all
        else all.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
        val q = query.trim()
        if (q.isEmpty()) return bySubject
        return bySubject.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.context.contains(q, ignoreCase = true)
        }
    }
}
