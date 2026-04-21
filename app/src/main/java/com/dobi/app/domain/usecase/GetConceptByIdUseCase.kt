package com.dobi.app.domain.usecase

import com.dobi.app.domain.model.Concept
import com.dobi.app.domain.repository.ConceptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConceptByIdUseCase @Inject constructor(
    private val repository: ConceptRepository
) {
    operator fun invoke(id: String): Flow<Concept?> = repository.getConceptById(id)
}
