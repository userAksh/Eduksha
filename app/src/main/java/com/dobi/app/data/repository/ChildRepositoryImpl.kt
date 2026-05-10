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
                if (error != null) { trySend(emptyList()); close(); return@addSnapshotListener }
                val children = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Child::class.java)
                } ?: emptyList()
                trySend(children)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getChild(parentId: String, childId: String): Result<Child> {
        return try {
            val doc = firestore.collection("parents").document(parentId)
                .collection("children").document(childId).get().await()
            val child = doc.toObject(Child::class.java)
            if (child != null) Result.success(child)
            else Result.failure(IllegalStateException("Child not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addChild(child: Child): Result<Child> {
        return try {
            val ref = firestore.collection("parents").document(child.parentId)
                .collection("children").document()
            val saved = child.copy(id = ref.id)
            ref.set(saved).await()
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateChild(child: Child): Result<Child> {
        return try {
            firestore.collection("parents").document(child.parentId)
                .collection("children").document(child.id)
                .set(child)
                .await()
            Result.success(child)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
