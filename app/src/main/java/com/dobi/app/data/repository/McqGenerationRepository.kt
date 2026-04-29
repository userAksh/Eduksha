package com.dobi.app.data.repository

import com.dobi.app.domain.model.McqQuestion
import com.dobi.app.domain.usecase.GenerateMcqRequest

interface McqGenerationRepository {
    suspend fun generate(request: GenerateMcqRequest): Result<List<McqQuestion>>
}
