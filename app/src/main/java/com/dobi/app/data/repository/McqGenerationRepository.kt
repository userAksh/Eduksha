package com.dobi.app.data.repository

import com.dobi.app.domain.model.McqQuestion
import com.dobi.app.domain.usecase.GenerateMcqRequest

interface McqGenerationRepository {
    suspend fun generate(request: GenerateMcqRequest): Result<List<McqQuestion>>

    /**
     * Text-only generation that produces MCQs from a lesson's syllabus
     * content (no photo needed). Used as the curated-quiz fallback when
     * the hand-written question bank doesn't cover a lesson.
     */
    suspend fun generateFromLesson(
        subject: String,
        grade: Int,
        unitName: String,
        conceptName: String,
        conceptDescription: String,
        targetCount: Int = 10
    ): Result<List<McqQuestion>>
}
