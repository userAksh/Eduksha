package com.dobi.app.domain.usecase

import com.dobi.app.data.repository.WriteAnswerRepository
import com.dobi.app.domain.model.GradingResult
import com.dobi.app.domain.model.SubjectiveQuestion
import javax.inject.Inject

class GenerateSubjectiveQuestionsUseCase @Inject constructor(
    private val repo: WriteAnswerRepository
) {
    suspend operator fun invoke(
        scopeText: String,
        subject: String,
        grade: Int,
        targetCount: Int
    ): Result<List<SubjectiveQuestion>> =
        repo.generateQuestions(scopeText, subject, grade, targetCount)
}

class GradeStructuredAnswersUseCase @Inject constructor(
    private val repo: WriteAnswerRepository
) {
    suspend operator fun invoke(
        scopeText: String,
        subject: String,
        grade: Int,
        questions: List<SubjectiveQuestion>,
        photoUrisByQuestionId: Map<String, List<String>>
    ): Result<GradingResult> =
        repo.gradeStructured(scopeText, subject, grade, questions, photoUrisByQuestionId)
}

class GradeNotebookAnswersUseCase @Inject constructor(
    private val repo: WriteAnswerRepository
) {
    suspend operator fun invoke(
        scopeText: String,
        subject: String,
        grade: Int,
        photoUris: List<String>
    ): Result<GradingResult> = repo.gradeFreeform(scopeText, subject, grade, photoUris)
}
