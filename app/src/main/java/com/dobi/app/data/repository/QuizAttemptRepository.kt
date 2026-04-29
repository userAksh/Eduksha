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
    private data class AttemptDoc(
        val attempts: Int = 0,
        val selections: Map<Int, Int> = emptyMap()
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

    suspend fun recordAttempt(scope: AttemptScope, targetId: String, selections: Map<Int, Int>) {
        val childId = userPreferences.linkedChildId.first().orEmpty()
        if (childId.isBlank() || targetId.isBlank()) return
        val docRef = firestore.collection("quiz_attempts")
            .document(childId)
            .collection(scope.collection)
            .document(targetId)

        firestore.runTransaction { tx ->
            val snap = tx.get(docRef)
            val current = snap.getLong("attempts")?.toInt() ?: 0
            tx.set(
                docRef,
                mapOf(
                    "attempts" to (current + 1).toLong(),
                    "selections" to encodeSelections(selections),
                    "lastUpdated" to System.currentTimeMillis()
                )
            )
            null
        }.await()
    }

    private fun attemptDocFlow(
        childId: String,
        scope: AttemptScope,
        targetId: String
    ): Flow<AttemptDoc> = callbackFlow {
        val docRef = firestore.collection("quiz_attempts")
            .document(childId)
            .collection(scope.collection)
            .document(targetId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error); return@addSnapshotListener
            }
            val attempts = snapshot?.getLong("attempts")?.toInt() ?: 0
            val selections = parseSelections(snapshot?.getString("selections").orEmpty())
            trySend(AttemptDoc(attempts, selections))
        }
        awaitClose { listener.remove() }
    }

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
