package com.dobi.app.data.repository

import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.model.RevisionMark
import com.dobi.app.domain.model.RevisionScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Tracks topics the child has flagged "Need Revision". Each mark gets a
 * deterministic doc id of `{childId}_{scope}_{targetId}` so toggling is
 * idempotent and we don't accumulate duplicate marks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RevisionMarkRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences
) {

    /** Returns the new mark state — true = now marked, false = unmarked. */
    suspend fun toggle(mark: RevisionMark): Result<Boolean> = runCatching {
        val childId = mark.childId.ifBlank {
            userPreferences.linkedChildId.first().orEmpty()
        }
        Log.d("RevisionMark", "toggle: childId=$childId scope=${mark.scope} targetId=${mark.targetId}")
        require(childId.isNotBlank()) { "No active child id" }
        require(mark.targetId.isNotBlank()) { "Missing target id" }
        val docId = docId(childId, mark.scope, mark.targetId)
        val docRef = firestore.collection("revision_marks").document(docId)
        val existing = suspendCancellableCoroutine<Boolean> { cont ->
            docRef.get()
                .addOnSuccessListener { snap -> cont.resume(snap.exists()) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        if (existing) {
            suspendCancellableCoroutine<Unit> { cont ->
                docRef.delete()
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            false
        } else {
            val data = mapOf(
                "id" to docId,
                "childId" to childId,
                "scope" to mark.scope,
                "targetId" to mark.targetId,
                "name" to mark.name,
                "subject" to mark.subject,
                "subjectColor" to mark.subjectColor,
                "subjectIcon" to mark.subjectIcon,
                "unitId" to mark.unitId,
                "unitName" to mark.unitName,
                "markedAt" to System.currentTimeMillis()
            )
            suspendCancellableCoroutine<Unit> { cont ->
                docRef.set(data)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            true
        }
    }.also { result ->
        result.onFailure { Log.e("RevisionMark", "toggle failed", it) }
    }

    fun isMarked(scope: RevisionScope, targetId: String): Flow<Boolean> =
        userPreferences.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank() || targetId.isBlank()) flowOf(false)
            else markFlow(childId, scope.key, targetId).map { it != null }
        }

    fun observeForActiveChild(): Flow<List<RevisionMark>> =
        userPreferences.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank()) flowOf(emptyList())
            else listFlow(childId)
        }

    private fun listFlow(childId: String): Flow<List<RevisionMark>> = callbackFlow {
        val listener = firestore.collection("revision_marks")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList()); close(); return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        RevisionMark(
                            id = doc.getString("id").orEmpty(),
                            childId = doc.getString("childId").orEmpty(),
                            scope = doc.getString("scope").orEmpty(),
                            targetId = doc.getString("targetId").orEmpty(),
                            name = doc.getString("name").orEmpty(),
                            subject = doc.getString("subject").orEmpty(),
                            subjectColor = doc.getString("subjectColor").orEmpty(),
                            subjectIcon = doc.getString("subjectIcon").orEmpty(),
                            unitId = doc.getString("unitId").orEmpty(),
                            unitName = doc.getString("unitName").orEmpty(),
                            markedAt = doc.getLong("markedAt") ?: 0L
                        )
                    }.getOrNull()
                }.sortedByDescending { it.markedAt }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    private fun markFlow(childId: String, scope: String, targetId: String): Flow<RevisionMark?> =
        callbackFlow {
            val docId = docId(childId, scope, targetId)
            val listener = firestore.collection("revision_marks").document(docId)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        trySend(null); close(); return@addSnapshotListener
                    }
                    val mark = if (snap != null && snap.exists()) {
                        RevisionMark(
                            id = snap.getString("id").orEmpty(),
                            childId = snap.getString("childId").orEmpty(),
                            scope = snap.getString("scope").orEmpty(),
                            targetId = snap.getString("targetId").orEmpty(),
                            name = snap.getString("name").orEmpty(),
                            subject = snap.getString("subject").orEmpty(),
                            subjectColor = snap.getString("subjectColor").orEmpty(),
                            subjectIcon = snap.getString("subjectIcon").orEmpty(),
                            unitId = snap.getString("unitId").orEmpty(),
                            unitName = snap.getString("unitName").orEmpty(),
                            markedAt = snap.getLong("markedAt") ?: 0L
                        )
                    } else null
                    trySend(mark)
                }
            awaitClose { listener.remove() }
        }

    private fun docId(childId: String, scope: String, targetId: String): String =
        "${childId}_${scope}_${targetId}"
}
