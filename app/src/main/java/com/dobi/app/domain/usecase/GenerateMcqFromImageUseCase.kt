package com.dobi.app.domain.usecase

import com.dobi.app.data.repository.McqGenerationRepository
import com.dobi.app.domain.model.McqQuestion
import javax.inject.Inject

class GenerateMcqFromImageUseCase @Inject constructor(
    private val repository: McqGenerationRepository
) {
    suspend operator fun invoke(
        request: GenerateMcqRequest
    ): Result<List<McqQuestion>> = repository.generate(request)
}

data class GenerateMcqRequest(
    val unitId: String,
    val unitName: String,
    val conceptId: String = "",
    val conceptName: String = "",
    val subject: String,
    val grade: Int,
    val photoUris: List<String>,
    val targetCount: Int = 8,
    val bloomMix: BloomMix = BloomMix.DEFAULT,
    val customScopeText: String? = null,
    val scopeDisplayName: String = ""
)

data class BloomMix(
    val recall: Int,
    val understand: Int,
    val apply: Int,
    val analyze: Int
) {
    companion object {
        val DEFAULT = BloomMix(recall = 2, understand = 2, apply = 2, analyze = 2)
    }
}
