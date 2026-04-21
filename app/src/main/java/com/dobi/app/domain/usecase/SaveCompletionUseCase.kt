package com.dobi.app.domain.usecase

import com.dobi.app.domain.model.Completion
import com.dobi.app.domain.repository.CompletionRepository
import javax.inject.Inject

class SaveCompletionUseCase @Inject constructor(
    private val repository: CompletionRepository
) {
    suspend operator fun invoke(completion: Completion) = repository.saveCompletion(completion)
}
