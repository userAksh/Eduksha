package com.dobi.app.domain.repository

import com.dobi.app.domain.model.Completion

interface CompletionRepository {
    suspend fun saveCompletion(completion: Completion)
}
