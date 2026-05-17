package com.dobi.app.data.repository

import com.dobi.app.domain.model.ActivityItem
import com.dobi.app.domain.model.ActivityKind
import com.dobi.app.domain.model.ChildInsights
import com.dobi.app.domain.model.InsightsRange
import com.dobi.app.domain.model.SubjectInsight
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
 * Streams a per-child [ChildInsights] snapshot for the parent dashboard's
 * Insights screen. The snapshot is recomputed live whenever any of the four
 * underlying Firestore sources updates: uploads, subjective_results,
 * `quiz_attempts/{childId}` subtree.
 *
 * The chosen [InsightsRange] sets the lower bound for "this period" — the
 * upper bound is always "now".
 *
 * Note: parent-task counts are NOT included here. The screen ViewModel
 * overlays those separately from `ParentTaskRepository.observeForActiveParent`.
 */
@Singleton
class ChildInsightsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun observeInsights(childId: String, range: InsightsRange): Flow<ChildInsights> {
        if (childId.isBlank()) return flowOf(ChildInsights(range = range))

        val rangeStart = rangeStartMillis(range)

        return combine(
            uploadDocsFlow(childId),
            subjectiveDocsFlow(childId),
            quizDocsFlow(childId)
        ) { uploads, subjectives, quizzes ->
            buildInsights(childId, range, rangeStart, uploads, subjectives, quizzes)
        }
    }

    private fun uploadDocsFlow(childId: String): Flow<List<UploadRow>> = callbackFlow {
        val listener = firestore.collection("uploads")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().mapNotNull { d ->
                    UploadRow(
                        title = (d.getString("topic")?.takeIf { it.isNotBlank() }
                            ?: d.getString("conceptName")?.takeIf { it.isNotBlank() }
                            ?: d.getString("unitName")
                            ?: "Untitled"),
                        subject = d.getString("subject").orEmpty(),
                        subjectColor = d.getString("subjectColor").orEmpty(),
                        unitOrConceptKey = listOf(
                            d.getString("conceptId").orEmpty(),
                            d.getString("unitId").orEmpty(),
                            d.getString("topic").orEmpty()
                        ).first { it.isNotBlank() }.ifBlank { d.id },
                        timestamp = d.getLong("timestamp") ?: 0L
                    )
                })
            }
        awaitClose { listener.remove() }
    }

    private fun subjectiveDocsFlow(childId: String): Flow<List<SubjectiveRow>> = callbackFlow {
        val listener = firestore.collection("subjective_results")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().mapNotNull { d ->
                    SubjectiveRow(
                        title = d.getString("displayName").orEmpty().ifBlank { "Written answer" },
                        subject = d.getString("subject").orEmpty(),
                        targetKey = d.getString("targetId")?.takeIf { it.isNotBlank() } ?: d.id,
                        timestamp = d.getLong("timestamp") ?: 0L,
                        attempted = (d.getLong("attemptedCount") ?: 0L).toInt(),
                        correct = (d.getLong("correctCount") ?: 0L).toInt(),
                        accuracyPct = (d.getLong("accuracyPct") ?: 0L).toInt()
                    )
                })
            }
        awaitClose { listener.remove() }
    }

    private fun quizDocsFlow(childId: String): Flow<List<QuizRow>> = callbackFlow {
        val map = mutableMapOf<String, QuizRow>()
        fun emit() = trySend(map.values.toList())
        val scopes = listOf("units", "concepts")
        val listeners = scopes.map { scope ->
            firestore.collection("quiz_attempts")
                .document(childId)
                .collection(scope)
                .addSnapshotListener { snap, error ->
                    if (error != null) { emit(); return@addSnapshotListener }
                    snap?.documents.orEmpty().forEach { d ->
                        map["$scope:${d.id}"] = QuizRow(
                            title = d.getString("displayName").orEmpty().ifBlank { "Quiz" },
                            subject = d.getString("subject").orEmpty(),
                            targetKey = "$scope:${d.id}",
                            timestamp = d.getLong("lastUpdated") ?: 0L,
                            correct = (d.getLong("lastCorrect") ?: 0L).toInt(),
                            total = (d.getLong("lastTotal") ?: 0L).toInt()
                        )
                    }
                    emit()
                }
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    private fun buildInsights(
        childId: String,
        range: InsightsRange,
        rangeStart: Long,
        uploads: List<UploadRow>,
        subjectives: List<SubjectiveRow>,
        quizzes: List<QuizRow>
    ): ChildInsights {
        val now = System.currentTimeMillis()
        val inRange: (Long) -> Boolean = { it in rangeStart..now }

        val uploadsInRange = uploads.filter { inRange(it.timestamp) }
        val subjectivesInRange = subjectives.filter { inRange(it.timestamp) }
        val quizzesInRange = quizzes.filter { inRange(it.timestamp) && it.total > 0 }

        // Days active across all sources (calendar-day buckets).
        val activeDays = buildSet<Long> {
            uploadsInRange.forEach { add(dayKey(it.timestamp)) }
            subjectivesInRange.forEach { add(dayKey(it.timestamp)) }
            quizzesInRange.forEach { add(dayKey(it.timestamp)) }
        }

        // Pooled accuracy across MCQs + subjectives.
        val totalCorrect = quizzesInRange.sumOf { it.correct } +
            subjectivesInRange.sumOf { it.correct }
        val totalAttempted = quizzesInRange.sumOf { it.total } +
            subjectivesInRange.sumOf { it.attempted }
        val accuracy = if (totalAttempted > 0)
            ((totalCorrect.toFloat() / totalAttempted) * 100f).toInt()
        else null

        // Subject roll-up.
        val perSubject = mutableMapOf<String, SubjectAccumulator>()
        fun bump(subject: String, key: String, correct: Int = 0, total: Int = 0) {
            val s = subject.trim().ifBlank { return }
            val acc = perSubject.getOrPut(s) { SubjectAccumulator() }
            acc.topics += key
            acc.activitiesCount += 1
            acc.correct += correct
            acc.total += total
        }
        uploadsInRange.forEach { bump(it.subject, it.unitOrConceptKey) }
        subjectivesInRange.forEach { bump(it.subject, it.targetKey, it.correct, it.attempted) }
        quizzesInRange.forEach { bump(it.subject, it.targetKey, it.correct, it.total) }

        val subjects = perSubject.entries
            .map { (name, acc) ->
                SubjectInsight(
                    name = name,
                    emoji = subjectEmoji(name),
                    colorHex = "",
                    topicsCount = acc.topics.size,
                    activitiesCount = acc.activitiesCount,
                    accuracyPct = if (acc.total > 0)
                        ((acc.correct.toFloat() / acc.total) * 100f).toInt() else null
                )
            }
            .sortedByDescending { it.activitiesCount }
            .take(6)

        // Recent activity timeline — newest first across all sources, up to 8.
        val recent = mutableListOf<ActivityItem>()
        uploadsInRange.forEach {
            recent += ActivityItem(
                kind = ActivityKind.UPLOAD,
                title = it.title,
                subject = it.subject,
                subjectEmoji = subjectEmoji(it.subject),
                subjectColorHex = it.subjectColor,
                timestampMillis = it.timestamp
            )
        }
        subjectivesInRange.forEach {
            recent += ActivityItem(
                kind = ActivityKind.WRITTEN,
                title = it.title,
                subject = it.subject,
                subjectEmoji = subjectEmoji(it.subject),
                subjectColorHex = "",
                timestampMillis = it.timestamp,
                accuracyPct = if (it.attempted > 0)
                    ((it.correct.toFloat() / it.attempted) * 100f).toInt() else null
            )
        }
        quizzesInRange.forEach {
            recent += ActivityItem(
                kind = ActivityKind.QUIZ,
                title = it.title,
                subject = it.subject,
                subjectEmoji = subjectEmoji(it.subject),
                subjectColorHex = "",
                timestampMillis = it.timestamp,
                accuracyPct = if (it.total > 0)
                    ((it.correct.toFloat() / it.total) * 100f).toInt() else null
            )
        }
        val recentSorted = recent.sortedByDescending { it.timestampMillis }.take(8)

        return ChildInsights(
            childId = childId,
            range = range,
            daysActive = activeDays.size,
            activitiesCompleted = uploadsInRange.size + subjectivesInRange.size + quizzesInRange.size,
            accuracyPct = accuracy,
            tasksDone = 0, // overlaid by ViewModel
            subjects = subjects,
            recentActivities = recentSorted
        )
    }

    // ── Internal compact row types — only the fields we actually aggregate. ──

    private data class UploadRow(
        val title: String,
        val subject: String,
        val subjectColor: String,
        val unitOrConceptKey: String,
        val timestamp: Long
    )

    private data class SubjectiveRow(
        val title: String,
        val subject: String,
        val targetKey: String,
        val timestamp: Long,
        val attempted: Int,
        val correct: Int,
        val accuracyPct: Int
    )

    private data class QuizRow(
        val title: String,
        val subject: String,
        val targetKey: String,
        val timestamp: Long,
        val correct: Int,
        val total: Int
    )

    private class SubjectAccumulator(
        val topics: MutableSet<String> = mutableSetOf(),
        var activitiesCount: Int = 0,
        var correct: Int = 0,
        var total: Int = 0
    )

    private fun rangeStartMillis(range: InsightsRange): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (range) {
            InsightsRange.TODAY -> Unit // already at today 00:00
            InsightsRange.WEEK -> cal.add(Calendar.DAY_OF_MONTH, -6)
            InsightsRange.MONTH -> cal.add(Calendar.DAY_OF_MONTH, -29)
        }
        return cal.timeInMillis
    }

    private fun dayKey(ts: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
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
        else -> "📚"
    }
}
