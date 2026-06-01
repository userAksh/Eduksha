package com.dobi.app.data.repository

import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.model.Difficulty
import com.dobi.app.domain.repository.ConceptRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConceptRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ConceptRepository {

    override fun getConcepts(grade: Int, board: String): Flow<List<Concept>> =
        firestore.collection("concepts")
            .where { "grade" equalTo grade }
            .where { "board" equalTo board }
            .snapshots
            .map { qs ->
                qs.documents.mapNotNull { doc ->
                    runCatching { doc.toConcept() }.getOrNull()
                }
            }

    override fun getConceptById(id: String): Flow<Concept?> =
        firestore.collection("concepts").document(id)
            .snapshots
            .map { snap -> if (snap.exists) runCatching { snap.toConcept() }.getOrNull() else null }
}

@Suppress("UNCHECKED_CAST")
private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toConcept() = Concept(
    id = get("id") as? String ?: id,
    name = get("name") as? String ?: "",
    description = get("description") as? String ?: "",
    subject = get("subject") as? String ?: "",
    subjectColor = get("subjectColor") as? String ?: "#22C55E",
    subjectIcon = get("subjectIcon") as? String ?: "🔬",
    unit = get("unit") as? String ?: "",
    unitId = get("unitId") as? String ?: "",
    unitOrder = (get("unitOrder") as? Long)?.toInt() ?: 0,
    grade = (get("grade") as? Long)?.toInt() ?: 6,
    board = get("board") as? String ?: "",
    expectedDifficulty = runCatching {
        Difficulty.valueOf(get("expectedDifficulty") as? String ?: "OK")
    }.getOrDefault(Difficulty.OK),
    aliases = runCatching { (get("aliases") as? List<*>)?.filterIsInstance<String>() }.getOrNull() ?: emptyList(),
    prerequisites = runCatching { (get("prerequisites") as? List<*>)?.filterIsInstance<String>() }.getOrNull() ?: emptyList(),
    order = (get("order") as? Long)?.toInt() ?: 0
)
