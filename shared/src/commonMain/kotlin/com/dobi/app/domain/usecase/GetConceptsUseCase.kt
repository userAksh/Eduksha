package com.dobi.app.domain.usecase

import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.repository.ConceptRepository
import kotlinx.coroutines.flow.Flow

class GetConceptsUseCase(
    private val repository: ConceptRepository
) {
    operator fun invoke(grade: Int, board: String): Flow<List<Concept>> =
        repository.getConcepts(grade, board)
}
