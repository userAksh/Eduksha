package com.dobi.app.data.repository

import com.dobi.app.domain.model.ChildEngagement
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Streams an aggregated engagement snapshot per child for the parent
 * dashboard tiles. Pulls activity rows (timestamp + score) from three
 * independent sources and combines them into a single [ChildEngagement] flow:
 *
 *   - `uploads` (whereEqualTo "childId") — timestamps only
 *   - `subjective_results` (whereEqualTo "childId") — timestamps + score
 *   - `quiz_attempts/{childId}/{units|concepts}` (subcollections) — score
 *
 * Day boundaries are computed in the device's local time zone — that's the
 * frame the parent reads "today" / "this week" in. Accuracy is pooled across
 * all scored attempts (no time-window filter), so the ring shows a stable
 * "how this child is doing" signal rather than a noisy weekly figure.
 */
@Singleton
class ChildEngagementRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun observeEngagement(childId: String): Flow<ChildEngagement> {
        if (childId.isBlank()) return flowOf(ChildEngagement())

        return combine(
            uploadTimestamps(childId),
            subjectiveRows(childId),
            quizRows(childId)
        ) { uploads, subjectives, quizzes ->
            buildEngagement(childId, uploads, subjectives, quizzes)
        }
    }

    private fun uploadTimestamps(childId: String): Flow<List<Long>> = callbackFlow {
        val listener = firestore.collection("uploads")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().mapNotNull { it.getLong("timestamp") })
            }
        awaitClose { listener.remove() }
    }

    private fun subjectiveRows(childId: String): Flow<List<ScoredRow>> = callbackFlow {
        val listener = firestore.collection("subjective_results")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().map { d ->
                    ScoredRow(
                        timestamp = d.getLong("timestamp") ?: 0L,
                        correct = (d.getLong("correctCount") ?: 0L).toInt(),
                        total = (d.getLong("attemptedCount") ?: 0L).toInt(),
                        subject = d.getString("subject").orEmpty()
                    )
                })
            }
        awaitClose { listener.remove() }
    }

    /**
     * Quiz attempts live at `quiz_attempts/{childId}/{units|concepts}/{targetId}`
     * — two parallel subcollections. Listen on both and merge.
     */
    private fun quizRows(childId: String): Flow<List<ScoredRow>> = callbackFlow {
        val collected = mutableMapOf<String, List<ScoredRow>>()
        fun emit() = trySend(collected.values.flatten())

        val scopes = listOf("units", "concepts")
        val listeners = scopes.map { scope ->
            firestore.collection("quiz_attempts")
                .document(childId)
                .collection(scope)
                .addSnapshotListener { snap, error ->
                    collected[scope] = if (error != null) {
                        emptyList()
                    } else {
                        snap?.documents.orEmpty().map { d ->
                            ScoredRow(
                                timestamp = d.getLong("lastUpdated") ?: 0L,
                                correct = (d.getLong("lastCorrect") ?: 0L).toInt(),
                                total = (d.getLong("lastTotal") ?: 0L).toInt(),
                                subject = d.getString("subject").orEmpty()
                            )
                        }
                    }
                    emit()
                }
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    private fun buildEngagement(
        childId: String,
        uploads: List<Long>,
        subjectives: List<ScoredRow>,
        quizzes: List<ScoredRow>
    ): ChildEngagement {
        val now = System.currentTimeMillis()
        val mondayMs = mondayOfThisWeekStart(now)
        val todayMs = todayStart(now)

        val timestamps = uploads +
            subjectives.map { it.timestamp } +
            quizzes.map { it.timestamp }

        val daysThisWeek = timestamps
            .asSequence()
            .filter { it in mondayMs..now }
            .map(::dayKey)
            .toSet()
            .size
            .coerceIn(0, 7)

        val isActiveToday = timestamps.any { it in todayMs..now }

        val epochDays = timestamps.filter { it > 0L }.map { epochDay(it) }.toSet()
        val streak = streakFromEpochDays(epochDays)

        val totalCorrect = subjectives.sumOf { it.correct } + quizzes.sumOf { it.correct }
        val totalAttempted = subjectives.sumOf { it.total } + quizzes.sumOf { it.total }
        val accuracy = if (totalAttempted > 0)
            ((totalCorrect.toFloat() / totalAttempted) * 100f).toInt().coerceIn(0, 100)
        else null

        // Per-subject accuracy: pool quiz + subjective rows, require MIN_ATTEMPTS threshold
        // before nominating a subject as "weak" to avoid noise from single-question outliers.
        val subjectStats = (subjectives + quizzes)
            .filter { it.subject.isNotBlank() && it.total > 0 }
            .groupBy { it.subject }
            .mapValues { (_, rows) ->
                val c = rows.sumOf { it.correct }
                val t = rows.sumOf { it.total }
                c to t
            }
            .filterValues { (_, t) -> t >= MIN_ATTEMPTS_FOR_WEAK_SUBJECT }
        val weakEntry = subjectStats.minByOrNull { (_, ct) -> ct.first.toFloat() / ct.second }
        val weakSubject = weakEntry?.key ?: ""
        val weakSubjectAccuracyPct = weakEntry?.value?.let { (c, t) ->
            ((c.toFloat() / t) * 100f).toInt().coerceIn(0, 100)
        }

        return ChildEngagement(
            childId = childId,
            daysActiveThisWeek = daysThisWeek,
            isActiveToday = isActiveToday,
            accuracyPct = accuracy,
            lastActivityMs = timestamps.maxOrNull() ?: 0L,
            streakDays = streak,
            weakSubject = weakSubject,
            weakSubjectAccuracyPct = weakSubjectAccuracyPct,
        )
    }

    companion object {
        private const val MIN_ATTEMPTS_FOR_WEAK_SUBJECT = 3
    }

    private data class ScoredRow(val timestamp: Long, val correct: Int, val total: Int, val subject: String = "")

    /** Stable per-calendar-day bucket for de-duping timestamps into "days". */
    private fun dayKey(ts: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
    }

    /** Epoch-day index (safe across year boundaries, unlike dayKey). */
    private fun epochDay(ts: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / 86_400_000L
    }

    private fun streakFromEpochDays(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        val today = epochDay(System.currentTimeMillis())
        var d = if (today !in days && (today - 1) in days) today - 1 else today
        var streak = 0
        while (d in days) { streak++; d-- }
        return streak
    }

    private fun todayStart(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun mondayOfThisWeekStart(now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2 … SATURDAY=7
        // Days to subtract to reach Monday: Mon=0, Tue=1, … Sat=5, Sun=6
        val daysToSubtract = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysToSubtract)
        return cal.timeInMillis
    }
}
