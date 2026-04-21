package com.dobi.app.data.repository

import com.dobi.app.domain.model.Completion
import com.dobi.app.domain.repository.CompletionRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CompletionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CompletionRepository {

    override suspend fun saveCompletion(completion: Completion) {
        val data = hashMapOf(
            "childId"   to completion.childId,
            "conceptId" to completion.conceptId,
            "effort"    to completion.effort.name,
            "mode"      to completion.mode.name,
            "timestamp" to Timestamp.now()
        )
        suspendCancellableCoroutine { cont ->
            firestore.collection("completions").add(data)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
