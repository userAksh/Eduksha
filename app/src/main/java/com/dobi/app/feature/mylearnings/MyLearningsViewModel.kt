package com.dobi.app.feature.mylearnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.AttemptScope
import com.dobi.app.data.repository.QuizAttemptRepository
import com.dobi.app.data.repository.RevisionMarkRepository
import com.dobi.app.data.repository.SubjectiveResultRepository
import com.dobi.app.data.repository.UploadEntryRepository
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.model.RevisionMark
import com.dobi.app.domain.model.RevisionScope
import com.dobi.app.domain.model.SubjectiveResult
import com.dobi.app.domain.model.UploadEntry
import com.dobi.app.domain.repository.DailyNewsRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private const val DAY_TICK_MS = 60_000L  // re-evaluate the day boundary every minute

@HiltViewModel
class MyLearningsViewModel @Inject constructor(
    private val revisionMarkRepository: RevisionMarkRepository,
    private val dailyNewsRepository: DailyNewsRepository,
    private val uploadEntryRepository: UploadEntryRepository,
    private val subjectiveResultRepository: SubjectiveResultRepository,
    private val parentTaskRepository: ParentTaskRepository,
    private val quizAttemptRepository: QuizAttemptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyLearningsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            revisionMarkRepository.observeForActiveChild()
                .catch { /* leave list empty on error */ }
                .collect { marks ->
                    _uiState.update { it.copy(revisionMarks = marks) }
                }
        }
        viewModelScope.launch {
            // The minute-ticker drives two things: (1) the calendar-day cutoff
            // recomputes so the section empties itself the moment the clock
            // crosses midnight, and (2) "5 min ago" labels stay fresh without
            // having to re-open the screen.
            combine(
                uploadEntryRepository.observeForActiveChild(),
                subjectiveResultRepository.observeForActiveChild(),
                parentTaskRepository.observeForActiveChild(),
                quizAttemptRepository.observeRecent(),
                minuteTicker()
            ) { uploads, subjectives, parentTasks, quizAttempts, now ->
                buildTodayItems(uploads, subjectives, parentTasks, quizAttempts, now)
            }
                .catch { /* leave list empty on error */ }
                .collect { items ->
                    _uiState.update { it.copy(todaysItems = items) }
                }
        }
        loadDailyNews()
    }

    fun onUnmark(mark: RevisionMark) {
        viewModelScope.launch {
            revisionMarkRepository.toggle(
                mark.copy(scope = RevisionScope.fromKey(mark.scope).key)
            )
        }
    }

    fun openStory(index: Int) {
        val state = _uiState.value
        if (state.currentAffairs.isEmpty()) {
            if (!state.isLoadingNews) loadDailyNews()
            return
        }
        val safeIndex = index.coerceIn(0, state.currentAffairs.size - 1)
        _uiState.update { it.copy(selectedStoryIndex = safeIndex) }
    }

    fun closeStory() {
        _uiState.update { it.copy(selectedStoryIndex = null) }
    }

    fun retryLoadNews() {
        if (_uiState.value.isLoadingNews) return
        loadDailyNews()
    }

    private fun loadDailyNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNews = true, newsError = null) }
            val result = dailyNewsRepository.fetchDailyNews()
            val stories = result.getOrNull().orEmpty()
            val mapped = stories.map { story ->
                CurrentAffair(
                    id = story.id,
                    headline = story.headline,
                    category = story.category,
                    timeAgo = "Today",
                    emoji = story.emoji,
                    body = story.body
                )
            }
            val error = when {
                result.isFailure -> result.exceptionOrNull()?.message
                    ?.takeIf { it.isNotBlank() } ?: "Couldn't reach the news service."
                mapped.isEmpty() -> "The paper isn't ready yet. Try again in a moment."
                else -> null
            }
            _uiState.update {
                it.copy(
                    currentAffairs = mapped,
                    isLoadingNews = false,
                    newsError = error
                )
            }
        }
    }
}

private fun minuteTicker(): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(DAY_TICK_MS)
    }
}

private fun startOfDayMillis(now: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = now
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun buildTodayItems(
    uploads: List<UploadEntry>,
    subjectives: List<SubjectiveResult>,
    parentTasks: List<ParentTask>,
    quizAttempts: List<QuizAttemptRepository.RecentQuizAttempt>,
    now: Long
): List<TodayLearningEntry> {
    val cutoff = startOfDayMillis(now)
    val items = mutableListOf<TodayLearningEntry>()

    uploads.asSequence()
        .filter { it.timestamp >= cutoff }
        .forEach { e ->
            items += TodayLearningEntry(
                id = "upload-${e.id}",
                kind = uploadKindToToday(e.kind),
                title = e.displayTitle,
                subject = e.subject.ifBlank { defaultSubjectFor(e.kind) },
                timeLabel = relativeTime(e.timestamp, now),
                status = TodayStatus.COMPLETED,
                timestamp = e.timestamp,
                navTarget = e.groupKey
                    .takeIf { it.isNotBlank() }
                    ?.let { TodayNavTarget.UploadGroup(it) }
            )
        }

    subjectives.asSequence()
        .filter { it.timestamp >= cutoff }
        .forEach { r ->
            items += TodayLearningEntry(
                id = "subj-${r.id}",
                kind = TodayKind.QUIZ,
                title = r.displayName.ifBlank { "Written answers" },
                subject = r.subject.ifBlank { "Self-study" },
                timeLabel = relativeTime(r.timestamp, now),
                status = TodayStatus.COMPLETED,
                scoreLabel = if (r.attemptedCount > 0) "${r.correctCount}/${r.attemptedCount}" else null,
                timestamp = r.timestamp,
                navTarget = r.id.takeIf { it.isNotBlank() }
                    ?.let { TodayNavTarget.Subjective(it) }
            )
        }

    quizAttempts.asSequence()
        .filter { it.lastUpdated >= cutoff && it.lastTotal > 0 }
        .forEach { q ->
            val (uid, cid) = if (q.scope == AttemptScope.UNIT) q.targetId to "" else "" to q.targetId
            items += TodayLearningEntry(
                id = "quiz-${q.scope.name}-${q.targetId}-${q.lastUpdated}",
                kind = TodayKind.QUIZ,
                title = q.displayName.ifBlank { "Quick quiz" },
                subject = q.subject.ifBlank { "Self-study" },
                timeLabel = relativeTime(q.lastUpdated, now),
                status = TodayStatus.COMPLETED,
                scoreLabel = "${q.lastCorrect}/${q.lastTotal}",
                timestamp = q.lastUpdated,
                navTarget = TodayNavTarget.QuizResult(unitId = uid, conceptId = cid)
            )
        }

    parentTasks.asSequence().forEach { t ->
        val activityAt = maxOf(
            t.requestedAtMillis,
            t.assignedAtMillis,
            t.submittedAtMillis,
            t.reviewedAtMillis
        )
        if (activityAt < cutoff) return@forEach
        val status = if (t.status == ParentTaskStatus.ACCEPTED)
            TodayStatus.COMPLETED else TodayStatus.PENDING
        val score = if (t.submissionScore != null && t.submissionTotal != null)
            "${t.submissionScore}/${t.submissionTotal}" else null
        items += TodayLearningEntry(
            id = "parent-${t.id}",
            kind = TodayKind.TASK,
            title = t.displayName.ifBlank { "Parent task" },
            subject = t.subject.ifBlank { "Family" },
            timeLabel = relativeTime(activityAt, now),
            status = status,
            scoreLabel = score,
            timestamp = activityAt,
            navTarget = t.id.takeIf { it.isNotBlank() }
                ?.let { TodayNavTarget.ParentTask(it) },
            assignedByParent = true
        )
    }

    return items
        .sortedByDescending { it.timestamp }
        .distinctBy { it.id }
}

private fun uploadKindToToday(kind: String): TodayKind = when {
    kind.equals("class_work", ignoreCase = true) -> TodayKind.HOMEWORK
    kind.equals("homework_log", ignoreCase = true) -> TodayKind.HOMEWORK
    kind.equals("homework", ignoreCase = true) -> TodayKind.HOMEWORK
    else -> TodayKind.TASK
}

private fun defaultSubjectFor(kind: String): String = when {
    kind.startsWith("tuition_", ignoreCase = true) -> "Tuition"
    kind.equals("ai_test_prep", ignoreCase = true) -> "Class test prep"
    kind.equals("self_learning", ignoreCase = true) -> "Self-study"
    else -> "Self-study"
}

private fun relativeTime(timestamp: Long, now: Long): String {
    val diff = (now - timestamp).coerceAtLeast(0)
    val mins = diff / 60_000L
    val hrs = diff / 3_600_000L
    return when {
        diff < 60_000L -> "Just now"
        mins < 60L -> "$mins min ago"
        hrs < 24L -> "$hrs hr ago"
        else -> "${hrs / 24} d ago"
    }
}
