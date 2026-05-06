package com.dobi.app.feature.childprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.data.repository.QuizAttemptRepository
import com.dobi.app.data.repository.SubjectiveResultRepository
import com.dobi.app.data.repository.UploadEntryRepository
import com.dobi.app.domain.model.SubjectiveResult
import com.dobi.app.domain.model.UploadEntry
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.DeleteAccountRepository
import com.dobi.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ChildProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val childRepository: ChildRepository,
    private val uploadEntryRepository: UploadEntryRepository,
    private val subjectiveResultRepository: SubjectiveResultRepository,
    private val quizAttemptRepository: QuizAttemptRepository,
    private val deleteAccountRepository: DeleteAccountRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildProfileUiState())
    val uiState = _uiState.asStateFlow()

    sealed interface DeletionEvent {
        data object Completed : DeletionEvent
        data class Failed(val message: String) : DeletionEvent
    }

    private val _deletionEvents = MutableSharedFlow<DeletionEvent>(extraBufferCapacity = 1)
    val deletionEvents = _deletionEvents.asSharedFlow()

    init {
        loadChild()
        observeStats()
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(deleteDialogVisible = true) }
    }

    fun dismissDeleteDialog() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(deleteDialogVisible = false) }
    }

    fun confirmDeleteAccount() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            val result = deleteAccountRepository.deleteAccount()
            result
                .onSuccess {
                    // Server side is wiped — clean local state too so the
                    // next cold start can't resurrect a stale session.
                    runCatching { userPreferences.clearAll() }
                    runCatching { auth.signOut() }
                    _uiState.update {
                        it.copy(isDeleting = false, deleteDialogVisible = false)
                    }
                    _deletionEvents.tryEmit(DeletionEvent.Completed)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isDeleting = false) }
                    _deletionEvents.tryEmit(
                        DeletionEvent.Failed(
                            err.message?.takeIf { it.isNotBlank() }
                                ?: "Couldn't delete the account. Please try again."
                        )
                    )
                }
        }
    }

    private fun loadChild() {
        viewModelScope.launch {
            val parentId = auth.currentUser?.uid
                ?: userRepository.linkedParentId.first()
                ?: return@launch
            val childId = userRepository.linkedChildId.first().orEmpty()
            if (childId.isBlank()) return@launch
            childRepository.getChild(parentId, childId)
                .onSuccess { child ->
                    _uiState.update {
                        it.copy(
                            name = child.name,
                            grade = child.grade,
                            board = child.board.ifBlank { "CBSE" },
                            gender = child.gender.ifBlank { "boy" },
                            friends = child.friends,
                            isLoading = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            combine(
                uploadEntryRepository.observeForActiveChild(),
                subjectiveResultRepository.observeForActiveChild(),
                quizAttemptRepository.observeRecent()
            ) { uploads, subjectives, quizzes ->
                computeStats(uploads, subjectives, quizzes)
            }
                .catch { /* leave defaults on error */ }
                .collect { stats ->
                    _uiState.update {
                        it.copy(
                            streakDays = stats.streakDays,
                            topicsCount = stats.topicsCount,
                            quizzesCount = stats.quizzesCount,
                            averageAccuracyPct = stats.averageAccuracyPct,
                            subjects = stats.subjects
                        )
                    }
                }
        }
    }
}

private data class ProfileStatsSnapshot(
    val streakDays: Int,
    val topicsCount: Int,
    val quizzesCount: Int,
    val averageAccuracyPct: Int,
    val subjects: List<SubjectStat>
)

private fun computeStats(
    uploads: List<UploadEntry>,
    subjectives: List<SubjectiveResult>,
    quizzes: List<QuizAttemptRepository.RecentQuizAttempt>
): ProfileStatsSnapshot {
    // Distinct topics — every upload group, plus every subjective + quiz target.
    val syllabusUploads = uploads.filter { it.unitId.isNotBlank() || it.conceptId.isNotBlank() }
    val topicKeys = mutableSetOf<String>()
    syllabusUploads.forEach { topicKeys += it.groupKey }
    subjectives.forEach { topicKeys += "subj-${it.targetId.ifBlank { it.id }}" }
    quizzes.filter { it.lastTotal > 0 }.forEach { topicKeys += "quiz-${it.scope.name}-${it.targetId}" }

    // Quizzes — every recorded attempt counts (curated + subjective).
    val quizzesCount = subjectives.size + quizzes.count { it.lastTotal > 0 }

    // Accuracy — pooled across both kinds (correct / total questions).
    val correctSum = subjectives.sumOf { it.correctCount } +
        quizzes.sumOf { it.lastCorrect }
    val totalSum = subjectives.sumOf { it.attemptedCount } +
        quizzes.sumOf { it.lastTotal }
    val avgAccuracy = if (totalSum > 0) ((correctSum.toFloat() / totalSum) * 100f).toInt() else 0

    // Streak — count consecutive days back from today with ANY activity.
    val activityDays = mutableSetOf<Long>()
    uploads.forEach { activityDays += dayIndex(it.timestamp) }
    subjectives.forEach { activityDays += dayIndex(it.timestamp) }
    quizzes.forEach { activityDays += dayIndex(it.lastUpdated) }
    val streak = streakFromDays(activityDays)

    // Subjects breakdown — group by subject across all sources.
    val perSubject = mutableMapOf<String, SubjectAcc>()
    fun bump(subject: String, key: String, correct: Int = 0, total: Int = 0) {
        val s = subject.trim().ifBlank { return }
        val acc = perSubject.getOrPut(s) { SubjectAcc() }
        acc.topics += key
        acc.correct += correct
        acc.total += total
    }
    syllabusUploads.forEach { bump(it.subject, it.groupKey) }
    subjectives.forEach {
        bump(it.subject, "subj-${it.id}", it.correctCount, it.attemptedCount)
    }
    quizzes.filter { it.lastTotal > 0 }.forEach {
        bump(it.subject, "quiz-${it.scope.name}-${it.targetId}", it.lastCorrect, it.lastTotal)
    }
    val subjectStats = perSubject.entries
        .map { (name, acc) ->
            SubjectStat(
                name = name,
                emoji = subjectEmoji(name),
                topicsCount = acc.topics.size,
                accuracyPct = if (acc.total > 0)
                    ((acc.correct.toFloat() / acc.total) * 100f).toInt()
                else null
            )
        }
        .sortedByDescending { it.topicsCount }
        .take(6)

    return ProfileStatsSnapshot(
        streakDays = streak,
        topicsCount = topicKeys.size,
        quizzesCount = quizzesCount,
        averageAccuracyPct = avgAccuracy,
        subjects = subjectStats
    )
}

private class SubjectAcc(
    val topics: MutableSet<String> = mutableSetOf(),
    var correct: Int = 0,
    var total: Int = 0
)

private fun dayIndex(ts: Long): Long {
    if (ts <= 0L) return 0L
    val cal = Calendar.getInstance()
    cal.timeInMillis = ts
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis / (24L * 60L * 60L * 1000L)
}

private fun streakFromDays(days: Set<Long>): Int {
    if (days.isEmpty()) return 0
    val today = dayIndex(System.currentTimeMillis())
    var streak = 0
    var d = today
    // Allow the streak to count yesterday even if there's no activity today
    // yet (kid hasn't done anything today but did yesterday → "1 day").
    if (today !in days && (today - 1) in days) d = today - 1
    while (d in days) {
        streak++
        d -= 1
    }
    return streak
}

private fun subjectEmoji(subject: String): String = when (subject.lowercase().trim()) {
    "science" -> "🔬"
    "physics" -> "⚛️"
    "chemistry" -> "🧪"
    "biology" -> "🌱"
    "maths", "mathematics", "math" -> "🧮"
    "english" -> "📖"
    "hindi" -> "✒️"
    "social science", "social studies", "social" -> "🌏"
    "history" -> "🏺"
    "geography" -> "🗺️"
    "civics" -> "⚖️"
    "computer", "computer science", "computers" -> "💻"
    "evs", "environmental studies" -> "🌳"
    "sanskrit" -> "📜"
    "tuition" -> "🎒"
    else -> "📘"
}
