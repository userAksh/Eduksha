package com.dobi.app.data.repository

import com.dobi.app.domain.model.Child
import com.dobi.app.domain.repository.ChildRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChildRepository {

    override fun getChildren(parentId: String): Flow<List<Child>> = callbackFlow {
        val listener = firestore.collection("parents").document(parentId)
            .collection("children")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val children = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Child::class.java)
                } ?: emptyList()
                trySend(children)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addChild(child: Child): Result<Unit> {
        return try {
            val ref = firestore.collection("parents").document(child.parentId)
                .collection("children").document()
            ref.set(child.copy(id = ref.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
