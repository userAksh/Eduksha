package com.dobi.app.data.repository

import com.dobi.app.domain.model.GradingResult
import com.dobi.app.domain.model.SubjectiveQuestion

interface WriteAnswerRepository {

    suspend fun generateQuestions(
        scopeText: String,
        subject: String,
        grade: Int,
        targetCount: Int
    ): Result<List<SubjectiveQuestion>>

    suspend fun gradeStructured(
        scopeText: String,
        subject: String,
        grade: Int,
        questions: List<SubjectiveQuestion>,
        photoUrisByQuestionId: Map<String, List<String>>
    ): Result<GradingResult>

    suspend fun gradeFreeform(
        scopeText: String,
        subject: String,
        grade: Int,
        photoUris: List<String>
    ): Result<GradingResult>
}
