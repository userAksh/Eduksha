package com.dobi.app.domain.usecase

import com.dobi.app.data.CuratedQuestionBank
import com.dobi.app.domain.model.McqQuestion
import javax.inject.Inject

class GetCuratedQuestionsUseCase @Inject constructor() {
    operator fun invoke(unitId: String): List<McqQuestion> =
        CuratedQuestionBank.forUnit(unitId)

    fun forConcept(conceptId: String): List<McqQuestion> =
        CuratedQuestionBank.forConcept(conceptId)
}
