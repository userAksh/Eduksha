package com.dobi.app.domain.repository

import com.dobi.app.domain.model.Homework

interface HomeworkRepository {
    suspend fun saveHomework(homework: Homework, localPhotoUriStrings: List<String>)
}
