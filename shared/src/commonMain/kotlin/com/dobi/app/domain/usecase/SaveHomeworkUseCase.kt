package com.dobi.app.domain.usecase

import com.dobi.app.domain.model.Homework
import com.dobi.app.domain.repository.HomeworkRepository

class SaveHomeworkUseCase(
    private val repository: HomeworkRepository
) {
    suspend operator fun invoke(
        homework: Homework,
        localPhotoUriStrings: List<String>
    ): List<String> = repository.saveHomework(homework, localPhotoUriStrings)
}
