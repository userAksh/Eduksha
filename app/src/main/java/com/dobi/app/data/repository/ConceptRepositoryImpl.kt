package com.dobi.app.data.repository

import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.model.Difficulty
import com.dobi.app.domain.repository.ConceptRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConceptRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ConceptRepository {

    override fun getConcepts(grade: Int, board: String): Flow<List<Concept>> = callbackFlow {
        val query = firestore.collection("concepts")
            .whereEqualTo("grade", grade)
            .whereEqualTo("board", board)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val concepts = snapshot?.documents?.mapNotNull { doc ->
                runCatching {
                    Concept(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        subject = doc.getString("subject") ?: "",
                        subjectColor = doc.getString("subjectColor") ?: "#22C55E",
                        subjectIcon = doc.getString("subjectIcon") ?: "🔬",
                        unit = doc.getString("unit") ?: "",
                        unitId = doc.getString("unitId") ?: "",
                        unitOrder = (doc.getLong("unitOrder") ?: 0L).toInt(),
                        grade = (doc.getLong("grade") ?: 6L).toInt(),
                        board = doc.getString("board") ?: "",
                        expectedDifficulty = runCatching {
                            Difficulty.valueOf(doc.getString("expectedDifficulty") ?: "OK")
                        }.getOrDefault(Difficulty.OK),
                        aliases = (doc.get("aliases") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        prerequisites = (doc.get("prerequisites") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        order = (doc.getLong("order") ?: 0L).toInt()
                    )
                }.getOrNull()
            } ?: emptyList()

            trySend(concepts)
        }

        awaitClose { listener.remove() }
    }

    override fun getConceptById(id: String): Flow<Concept?> = callbackFlow {
        val docRef = firestore.collection("concepts").document(id)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val concept = if (snapshot != null && snapshot.exists()) {
                runCatching {
                    Concept(
                        id = snapshot.getString("id") ?: snapshot.id,
                        name = snapshot.getString("name") ?: "",
                        description = snapshot.getString("description") ?: "",
                        subject = snapshot.getString("subject") ?: "",
                        subjectColor = snapshot.getString("subjectColor") ?: "#22C55E",
                        subjectIcon = snapshot.getString("subjectIcon") ?: "🔬",
                        unit = snapshot.getString("unit") ?: "",
                        unitId = snapshot.getString("unitId") ?: "",
                        unitOrder = (snapshot.getLong("unitOrder") ?: 0L).toInt(),
                        grade = (snapshot.getLong("grade") ?: 6L).toInt(),
                        board = snapshot.getString("board") ?: "",
                        expectedDifficulty = runCatching {
                            Difficulty.valueOf(snapshot.getString("expectedDifficulty") ?: "OK")
                        }.getOrDefault(Difficulty.OK),
                        aliases = (snapshot.get("aliases") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        prerequisites = (snapshot.get("prerequisites") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        order = (snapshot.getLong("order") ?: 0L).toInt()
                    )
                }.getOrNull()
            } else null
            trySend(concept)
        }

        awaitClose { listener.remove() }
    }
}
