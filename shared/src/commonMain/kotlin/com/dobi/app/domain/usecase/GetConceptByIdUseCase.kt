package com.dobi.app.domain.usecase

import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.repository.ConceptRepository
import kotlinx.coroutines.flow.Flow

class GetConceptByIdUseCase(
    private val repository: ConceptRepository
) {
    operator fun invoke(id: String): Flow<Concept?> = repository.getConceptById(id)
}
