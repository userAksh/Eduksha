package com.dobi.app.data.repository

import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.model.SubjectiveResult
import com.dobi.app.domain.model.SubjectiveResultItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SubjectiveResultRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences
) {

    suspend fun save(result: SubjectiveResult): Result<SubjectiveResult> = runCatching {
        val childId = result.childId.ifBlank {
            userPreferences.linkedChildId.first().orEmpty()
        }
        require(childId.isNotBlank()) { "No active child id" }
        val docRef = firestore.collection("subjective_results").document()
        val saved = result.copy(
            id = docRef.id,
            childId = childId,
            timestamp = if (result.timestamp > 0) result.timestamp else System.currentTimeMillis()
        )
        suspendCancellableCoroutine<Unit> { cont ->
            docRef.set(toMap(saved))
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        saved
    }

    fun observeForActiveChild(): Flow<List<SubjectiveResult>> =
        userPreferences.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank()) flowOf(emptyList())
            else listFlow(childId)
        }

    suspend fun getById(id: String): Result<SubjectiveResult> = runCatching {
        suspendCancellableCoroutine { cont ->
            firestore.collection("subjective_results").document(id).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists()) cont.resume(parse(snap.id, snap.data.orEmpty()))
                    else cont.resumeWithException(IllegalStateException("Result not found"))
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    private fun listFlow(childId: String): Flow<List<SubjectiveResult>> = callbackFlow {
        val listener = firestore.collection("subjective_results")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { parse(doc.id, doc.data.orEmpty()) }.getOrNull()
                }.sortedByDescending { it.timestamp }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    private fun toMap(r: SubjectiveResult): Map<String, Any?> = mapOf(
        "id" to r.id,
        "childId" to r.childId,
        "scope" to r.scope,
        "targetId" to r.targetId,
        "displayName" to r.displayName,
        "subject" to r.subject,
        "unitId" to r.unitId,
        "unitName" to r.unitName,
        "conceptId" to r.conceptId,
        "conceptName" to r.conceptName,
        "timestamp" to r.timestamp,
        "attemptedCount" to r.attemptedCount,
        "correctCount" to r.correctCount,
        "accuracyPct" to r.accuracyPct,
        "items" to r.items.map { itemToMap(it) },
        "createdAt" to Timestamp.now()
    )

    private fun itemToMap(item: SubjectiveResultItem): Map<String, Any?> = mapOf(
        "questionId" to item.questionId,
        "questionText" to item.questionText,
        "answerSeen" to item.answerSeen,
        "isAttempted" to item.isAttempted,
        "isCorrect" to item.isCorrect,
        "score" to item.score.toDouble(),
        "softVerdict" to item.softVerdict,
        "strictFeedback" to item.strictFeedback
    )

    @Suppress("UNCHECKED_CAST")
    private fun parse(id: String, data: Map<String, Any?>): SubjectiveResult = SubjectiveResult(
        id = (data["id"] as? String).orEmpty().ifBlank { id },
        childId = (data["childId"] as? String).orEmpty(),
        scope = (data["scope"] as? String).orEmpty(),
        targetId = (data["targetId"] as? String).orEmpty(),
        displayName = (data["displayName"] as? String).orEmpty(),
        subject = (data["subject"] as? String).orEmpty(),
        unitId = (data["unitId"] as? String).orEmpty(),
        unitName = (data["unitName"] as? String).orEmpty(),
        conceptId = (data["conceptId"] as? String).orEmpty(),
        conceptName = (data["conceptName"] as? String).orEmpty(),
        timestamp = (data["timestamp"] as? Long) ?: 0L,
        attemptedCount = ((data["attemptedCount"] as? Long) ?: 0L).toInt(),
        correctCount = ((data["correctCount"] as? Long) ?: 0L).toInt(),
        accuracyPct = ((data["accuracyPct"] as? Long) ?: 0L).toInt(),
        items = (data["items"] as? List<Map<String, Any?>>).orEmpty().map { itm ->
            SubjectiveResultItem(
                questionId = (itm["questionId"] as? String).orEmpty(),
                questionText = (itm["questionText"] as? String).orEmpty(),
                answerSeen = (itm["answerSeen"] as? String).orEmpty(),
                isAttempted = (itm["isAttempted"] as? Boolean) ?: false,
                isCorrect = (itm["isCorrect"] as? Boolean) ?: false,
                score = ((itm["score"] as? Number)?.toFloat()) ?: 0f,
                softVerdict = (itm["softVerdict"] as? String).orEmpty(),
                strictFeedback = (itm["strictFeedback"] as? String).orEmpty()
            )
        }
    )
}
