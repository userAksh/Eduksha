package com.dobi.app.domain.repository

import com.dobi.app.domain.model.Concept
import kotlinx.coroutines.flow.Flow

interface ConceptRepository {
    fun getConcepts(grade: Int, board: String): Flow<List<Concept>>
    fun getConceptById(id: String): Flow<Concept?>
}
