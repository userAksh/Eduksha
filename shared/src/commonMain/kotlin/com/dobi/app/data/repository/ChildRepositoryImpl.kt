package com.dobi.app.data.repository

import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.Friend
import com.dobi.app.domain.repository.ChildRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChildRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ChildRepository {

    override fun getChildren(parentId: String): Flow<List<Child>> =
        firestore.collection("parents").document(parentId)
            .collection("children")
            .snapshots
            .map { qs -> qs.documents.mapNotNull { runCatching { it.toChild() }.getOrNull() } }

    override suspend fun getChild(parentId: String, childId: String): Result<Child> = runCatching {
        val doc = firestore.collection("parents").document(parentId)
            .collection("children").document(childId).get()
        if (doc.exists) doc.toChild()
        else error("Child not found")
    }

    override suspend fun addChild(child: Child): Result<Child> = runCatching {
        val colRef = firestore.collection("parents").document(child.parentId).collection("children")
        val ref = colRef.document
        val saved = child.copy(id = ref.id)
        ref.set(saved.toMap())
        saved
    }

    override suspend fun updateChild(child: Child): Result<Child> = runCatching {
        firestore.collection("parents").document(child.parentId)
            .collection("children").document(child.id)
            .set(child.toMap())
        child
    }
}

private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toChild() = Child(
    id = get("id") as? String ?: id,
    name = get("name") as? String ?: "",
    grade = (get("grade") as? Long)?.toInt() ?: 1,
    board = get("board") as? String ?: "CBSE",
    parentId = get("parentId") as? String ?: "",
    gender = get("gender") as? String ?: "boy",
    dateOfBirth = get("dateOfBirth") as? String ?: "",
    pin = get("pin") as? String ?: "",
    friends = runCatching { (get("friends") as? List<*>)?.mapNotNull { it.toFriend() } }.getOrNull() ?: emptyList()
)

@Suppress("UNCHECKED_CAST")
private fun Any?.toFriend(): Friend? {
    val m = this as? Map<String, Any?> ?: return null
    return Friend(
        parentId = m["parentId"] as? String ?: "",
        childId = m["childId"] as? String ?: "",
        name = m["name"] as? String ?: "",
        grade = (m["grade"] as? Long)?.toInt() ?: 0,
        gender = m["gender"] as? String ?: "",
        addedAtMillis = m["addedAtMillis"] as? Long ?: 0L
    )
}

private fun Child.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "grade" to grade,
    "board" to board,
    "parentId" to parentId,
    "gender" to gender,
    "dateOfBirth" to dateOfBirth,
    "pin" to pin,
    "friends" to friends.map { f ->
        mapOf(
            "parentId" to f.parentId,
            "childId" to f.childId,
            "name" to f.name,
            "grade" to f.grade,
            "gender" to f.gender,
            "addedAtMillis" to f.addedAtMillis
        )
    }
)
