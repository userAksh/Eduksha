package com.dobi.app.data.repository

import com.dobi.app.data.local.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

enum class AttemptScope(val collection: String) {
    UNIT("units"),
    CONCEPT("concepts")
}

/**
 * Per-child, per-target (unit OR concept) quiz attempt tracking. Lives in Firestore
 * so the cap survives uninstall + reinstall as long as the same child profile is
 * paired again.
 *
 * Path: quiz_attempts/{childId}/{units|concepts}/{targetId}
 *   - attempts: Long
 *   - selections: String (e.g. "0:2,1:0,2:3")
 *   - lastUpdated: Long (epoch ms)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class QuizAttemptRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences
) {
    data class QuizAttemptSummary(
        val attempts: Int = 0,
        val selections: Map<Int, Int> = emptyMap(),
        val lastCorrect: Int = 0,
        val lastTotal: Int = 0,
        val lastUpdated: Long = 0L,
        val displayName: String = "",
        val subject: String = ""
    ) {
        val hasResult: Boolean get() = attempts > 0 && lastTotal > 0
        val accuracyPct: Int get() =
            if (lastTotal == 0) 0
            else ((lastCorrect.toFloat() / lastTotal) * 100f).toInt()
    }

    /** Flat row used by the My Learnings → Today's Learning aggregator. */
    data class RecentQuizAttempt(
        val scope: AttemptScope,
        val targetId: String,
        val displayName: String,
        val subject: String,
        val lastCorrect: Int,
        val lastTotal: Int,
        val lastUpdated: Long
    )

    fun attempts(scope: AttemptScope, targetId: String): Flow<Int> =
        userPreferences.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank() || targetId.isBlank()) flowOf(0)
            else attemptDocFlow(childId, scope, targetId).map { it.attempts }
        }

    fun lastSelections(scope: AttemptScope, targetId: String): Flow<Map<Int, Int>> =
        userPreferences.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank() || targetId.isBlank()) flowOf(emptyMap())
            else attemptDocFlow(childId, scope, targetId).map { it.selections }
        }

    /**
     * Child-explicit variants used by the parent dashboard's view-result flow.
     * The default [attempts] / [lastSelections] above look up the active child
     * via UserPreferences — that's blank on the parent device, so we need to
     * pass childId in explicitly.
     */
    fun attemptsForChild(childId: String, scope: AttemptScope, targetId: String): Flow<Int> =
        if (childId.isBlank() || targetId.isBlank()) flowOf(0)
        else attemptDocFlow(childId, scope, targetId).map { it.attempts }

    fun lastSelectionsForChild(
        childId: String,
        scope: AttemptScope,
        targetId: String
    ): Flow<Map<Int, Int>> =
        if (childId.isBlank() || targetId.isBlank()) flowOf(emptyMap())
        else attemptDocFlow(childId, scope, targetId).map { it.selections }

    fun summaryForChild(
        childId: String,
        scope: AttemptScope,
        targetId: String
    ): Flow<QuizAttemptSummary> =
        if (childId.isBlank() || targetId.isBlank()) flowOf(QuizAttemptSummary())
        else attemptDocFlow(childId, scope, targetId)

    /** All summaries across both unit + concept scopes for the active child. */
    fun observeAllSummaries(): Flow<Map<Pair<AttemptScope, String>, QuizAttemptSummary>> =
        userPreferences.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank()) flowOf(emptyMap())
            else allSummariesFlow(childId)
        }

    suspend fun recordAttempt(
        scope: AttemptScope,
        targetId: String,
        selections: Map<Int, Int>,
        correctCount: Int = 0,
        totalQuestions: Int = 0,
        displayName: String = "",
        subject: String = ""
    ) {
        val childId = userPreferences.linkedChildId.first().orEmpty()
        if (childId.isBlank() || targetId.isBlank()) return
        val docRef = firestore.collection("quiz_attempts")
            .document(childId)
            .collection(scope.collection)
            .document(targetId)

        firestore.runTransaction { tx ->
            val snap = tx.get(docRef)
            val current = snap.getLong("attempts")?.toInt() ?: 0
            // Preserve previously-stored display fields if the new caller
            // didn't pass them (lets older call-sites update score without
            // wiping the lesson title).
            val existingDisplay = snap.getString("displayName").orEmpty()
            val existingSubject = snap.getString("subject").orEmpty()
            val nextDisplay = displayName.ifBlank { existingDisplay }
            val nextSubject = subject.ifBlank { existingSubject }
            tx.set(
                docRef,
                mapOf(
                    "attempts" to (current + 1).toLong(),
                    "selections" to encodeSelections(selections),
                    "lastCorrect" to correctCount.toLong(),
                    "lastTotal" to totalQuestions.toLong(),
                    "lastUpdated" to System.currentTimeMillis(),
                    "displayName" to nextDisplay,
                    "subject" to nextSubject
                )
            )
            null
        }.await()
    }

    /**
     * Streams a flat list of every quiz attempt across both unit and concept
     * scopes for the active child, with display fields baked in. Used by
     * Today's Learning to surface curated MCQ attempts (which never produce
     * an UploadEntry).
     */
    fun observeRecent(): Flow<List<RecentQuizAttempt>> =
        userPreferences.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank()) flowOf(emptyList())
            else recentFlow(childId)
        }

    private fun recentFlow(childId: String): Flow<List<RecentQuizAttempt>> = callbackFlow {
        val map = mutableMapOf<Pair<AttemptScope, String>, RecentQuizAttempt>()
        fun emit() = trySend(map.values.toList())

        val listeners = AttemptScope.values().map { scope ->
            firestore.collection("quiz_attempts")
                .document(childId)
                .collection(scope.collection)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        emit(); close(); return@addSnapshotListener
                    }
                    snap?.documents.orEmpty().forEach { doc ->
                        map[scope to doc.id] = RecentQuizAttempt(
                            scope = scope,
                            targetId = doc.id,
                            displayName = doc.getString("displayName").orEmpty(),
                            subject = doc.getString("subject").orEmpty(),
                            lastCorrect = (doc.getLong("lastCorrect") ?: 0L).toInt(),
                            lastTotal = (doc.getLong("lastTotal") ?: 0L).toInt(),
                            lastUpdated = doc.getLong("lastUpdated") ?: 0L
                        )
                    }
                    emit()
                }
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    private fun attemptDocFlow(
        childId: String,
        scope: AttemptScope,
        targetId: String
    ): Flow<QuizAttemptSummary> = callbackFlow {
        val docRef = firestore.collection("quiz_attempts")
            .document(childId)
            .collection(scope.collection)
            .document(targetId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(QuizAttemptSummary()); close(); return@addSnapshotListener
            }
            trySend(snapshot?.let(::toSummary) ?: QuizAttemptSummary())
        }
        awaitClose { listener.remove() }
    }

    private fun allSummariesFlow(
        childId: String
    ): Flow<Map<Pair<AttemptScope, String>, QuizAttemptSummary>> = callbackFlow {
        val map = mutableMapOf<Pair<AttemptScope, String>, QuizAttemptSummary>()
        fun emit() = trySend(map.toMap())

        val listeners = AttemptScope.values().map { scope ->
            firestore.collection("quiz_attempts")
                .document(childId)
                .collection(scope.collection)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        emit(); close(); return@addSnapshotListener
                    }
                    snap?.documents.orEmpty().forEach { doc ->
                        map[scope to doc.id] = toSummary(doc)
                    }
                    emit()
                }
        }
        awaitClose { listeners.forEach { it.remove() } }
    }

    private fun toSummary(snap: com.google.firebase.firestore.DocumentSnapshot): QuizAttemptSummary =
        QuizAttemptSummary(
            attempts = snap.getLong("attempts")?.toInt() ?: 0,
            selections = parseSelections(snap.getString("selections").orEmpty()),
            lastCorrect = snap.getLong("lastCorrect")?.toInt() ?: 0,
            lastTotal = snap.getLong("lastTotal")?.toInt() ?: 0,
            lastUpdated = snap.getLong("lastUpdated") ?: 0L,
            displayName = snap.getString("displayName").orEmpty(),
            subject = snap.getString("subject").orEmpty()
        )

    private fun encodeSelections(selections: Map<Int, Int>): String =
        selections.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun parseSelections(raw: String): Map<Int, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val q = parts[0].toIntOrNull() ?: return@mapNotNull null
            val o = parts[1].toIntOrNull() ?: return@mapNotNull null
            q to o
        }.toMap()
    }
}
