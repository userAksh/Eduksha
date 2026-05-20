package com.dobi.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified activity-log feed surfaced to the parent dashboard. Combines three
 * Firestore sources into a single timestamped stream:
 *
 *   - uploads/{childId}             — every photo upload (school/tuition/self)
 *   - subjective_results/{childId}  — write-answer / class-test-prep sessions
 *   - quiz_attempts/{childId}/...   — curated + AI MCQ attempt rolls
 *
 * The repository is read-only — writes happen in the per-source repos.
 *
 * Category buckets (matching the three buttons on the dashboard):
 *   - SCHOOL_WORK    upload kinds class_work + tuition_work_*
 *   - HOME_WORK      upload kinds tuition_hw_*
 *   - SELF_LEARNING  upload kinds self_learning + ai_test_prep, plus all quiz
 *                    attempts and all subjective results (student-driven study)
 */
@Singleton
class ActivityLogRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    enum class Category(val key: String) {
        SCHOOL_WORK("school_work"),
        HOME_WORK("home_work"),
        SELF_LEARNING("self_learning");

        companion object {
            fun fromKey(key: String): Category? = values().firstOrNull { it.key == key }
        }
    }

    enum class Source { UPLOAD, QUIZ_MCQ, QUIZ_SUBJECTIVE, HOMEWORK }

    data class ScoreInfo(val correct: Int, val total: Int)

    /**
     * Anything the parent can tap to drill into. The detail-route fields are
     * the inputs the existing per-source detail screens already accept.
     */
    sealed class DetailRoute {
        data class Upload(
            val groupKey: String,
            val photoUrls: List<String>,
        ) : DetailRoute()

        data class QuizMcq(
            val unitId: String,
            val conceptId: String,
        ) : DetailRoute()

        data class QuizSubjective(
            val resultId: String,
        ) : DetailRoute()

        data class Homework(
            val homeworkId: String,
        ) : DetailRoute()
    }

    data class ActivityLogItem(
        val id: String,
        val source: Source,
        val category: Category,
        val title: String,
        val subject: String,
        val uploadKind: String,
        val timestamp: Long,
        val score: ScoreInfo?,
        val photoUrls: List<String>,
        val detail: DetailRoute,
    )

    /**
     * Emits the full unfiltered activity list for [childId]. The screen applies
     * category + time-window filters in the VM (cheaper than re-querying).
     */
    fun observeForChild(childId: String): Flow<List<ActivityLogItem>> {
        if (childId.isBlank()) return flowOf(emptyList())
        return combine(
            uploadsFlow(childId),
            subjectiveFlow(childId),
            quizFlow(childId),
            homeworksFlow(childId)
        ) { uploads, subjective, quizzes, homeworks ->
            (uploads + subjective + quizzes + homeworks).sortedByDescending { it.timestamp }
        }
    }

    private fun uploadsFlow(childId: String): Flow<List<ActivityLogItem>> = callbackFlow {
        val listener = firestore.collection("uploads")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val items = snap?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        val kind = doc.getString("kind").orEmpty()
                        val category = categoryForUploadKind(kind) ?: return@runCatching null
                        val photoUrls = (doc.get("photoUrls") as? List<*>).orEmpty()
                            .filterIsInstance<String>()
                        val topic = doc.getString("topic").orEmpty()
                        val conceptName = doc.getString("conceptName").orEmpty()
                        val unitName = doc.getString("unitName").orEmpty()
                        val title = topic.ifBlank { conceptName.ifBlank { unitName } }
                            .ifBlank { "Untitled upload" }
                        val groupKey = uploadGroupKey(
                            id = doc.id,
                            topic = topic,
                            subject = doc.getString("subject").orEmpty(),
                            conceptId = doc.getString("conceptId").orEmpty(),
                            unitId = doc.getString("unitId").orEmpty(),
                        )
                        ActivityLogItem(
                            id = doc.id,
                            source = Source.UPLOAD,
                            category = category,
                            title = title,
                            subject = doc.getString("subject").orEmpty(),
                            uploadKind = kind,
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            score = null,
                            photoUrls = photoUrls,
                            detail = DetailRoute.Upload(groupKey, photoUrls),
                        )
                    }.getOrNull()
                }
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    private fun subjectiveFlow(childId: String): Flow<List<ActivityLogItem>> = callbackFlow {
        val listener = firestore.collection("subjective_results")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val items = snap?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        val title = doc.getString("displayName").orEmpty()
                            .ifBlank { "Write-answer session" }
                        ActivityLogItem(
                            id = doc.id,
                            source = Source.QUIZ_SUBJECTIVE,
                            category = Category.SELF_LEARNING,
                            title = title,
                            subject = doc.getString("subject").orEmpty(),
                            uploadKind = "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            score = ScoreInfo(
                                correct = ((doc.getLong("correctCount") ?: 0L).toInt()),
                                total = ((doc.getLong("attemptedCount") ?: 0L).toInt()),
                            ),
                            photoUrls = emptyList(),
                            detail = DetailRoute.QuizSubjective(doc.id),
                        )
                    }.getOrNull()
                }
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Quiz attempts live in two parallel subcollections (units + concepts).
     * Listen to both and merge.
     */
    private fun quizFlow(childId: String): Flow<List<ActivityLogItem>> = callbackFlow {
        val collected = mutableMapOf<String, List<ActivityLogItem>>()
        fun emit() = trySend(collected.values.flatten())

        val scopes = listOf("units" to true, "concepts" to false)
        val listeners = scopes.map { (scope, isUnitScope) ->
            firestore.collection("quiz_attempts")
                .document(childId)
                .collection(scope)
                .addSnapshotListener { snap, error ->
                    collected[scope] = if (error != null) {
                        emptyList()
                    } else {
                        snap?.documents.orEmpty().mapNotNull { doc ->
                            runCatching {
                                val attempts = doc.getLong("attempts")?.toInt() ?: 0
                                if (attempts <= 0) return@runCatching null
                                val total = (doc.getLong("lastTotal") ?: 0L).toInt()
                                val correct = (doc.getLong("lastCorrect") ?: 0L).toInt()
                                val displayName = doc.getString("displayName").orEmpty()
                                    .ifBlank { "Quiz" }
                                ActivityLogItem(
                                    id = doc.id,
                                    source = Source.QUIZ_MCQ,
                                    category = Category.SELF_LEARNING,
                                    title = displayName,
                                    subject = doc.getString("subject").orEmpty(),
                                    uploadKind = "",
                                    timestamp = doc.getLong("lastUpdated") ?: 0L,
                                    score = if (total > 0) ScoreInfo(correct, total) else null,
                                    photoUrls = emptyList(),
                                    detail = if (isUnitScope) {
                                        DetailRoute.QuizMcq(unitId = doc.id, conceptId = "")
                                    } else {
                                        DetailRoute.QuizMcq(unitId = "", conceptId = doc.id)
                                    },
                                )
                            }.getOrNull()
                        }
                    }
                    emit()
                }
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    /**
     * `homeworks` is the school-homework log written by [HomeworkRepositoryImpl].
     * The `timestamp` field is stored as a Firestore Timestamp here (vs the
     * Long used by `uploads`), so we convert via toDate().time. Falls back to
     * the ISO `date` string when the doc was written before timestamp existed.
     */
    private fun homeworksFlow(childId: String): Flow<List<ActivityLogItem>> = callbackFlow {
        val listener = firestore.collection("homeworks")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val items = snap?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        val subject = doc.getString("subject").orEmpty()
                        val teacherName = doc.getString("teacherName").orEmpty()
                        val date = doc.getString("date").orEmpty()
                        val photoUrls = (doc.get("photoUrls") as? List<*>).orEmpty()
                            .filterIsInstance<String>()
                        val ts = (doc.get("timestamp") as? Timestamp)?.toDate()?.time
                            ?: parseIsoDate(date)
                        val title = when {
                            subject.isNotBlank() && teacherName.isNotBlank() ->
                                "$subject homework — $teacherName"
                            subject.isNotBlank() -> "$subject homework"
                            else -> "School homework"
                        }
                        ActivityLogItem(
                            id = doc.id,
                            source = Source.HOMEWORK,
                            category = Category.HOME_WORK,
                            title = title,
                            subject = subject,
                            uploadKind = "school_homework",
                            timestamp = ts,
                            score = null,
                            photoUrls = photoUrls,
                            detail = DetailRoute.Homework(doc.id),
                        )
                    }.getOrNull()
                }
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    private fun parseIsoDate(date: String): Long = runCatching {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.parse(date)?.time ?: 0L
    }.getOrDefault(0L)

    private fun uploadGroupKey(
        id: String,
        topic: String,
        subject: String,
        conceptId: String,
        unitId: String
    ): String = when {
        topic.isNotBlank() -> "tuition:${subject.lowercase()}:${topic.lowercase()}"
        conceptId.isNotBlank() -> "concept:$conceptId"
        unitId.isNotBlank() -> "unit:$unitId"
        else -> "unknown:$id"
    }

    private fun categoryForUploadKind(kind: String): Category? = when (kind) {
        "class_work",
        "tuition_work_learning",
        "tuition_work_written",
        "tuition_work_test" -> Category.SCHOOL_WORK
        "tuition_hw_learning",
        "tuition_hw_written" -> Category.HOME_WORK
        "self_learning",
        "ai_test_prep" -> Category.SELF_LEARNING
        else -> null
    }
}
