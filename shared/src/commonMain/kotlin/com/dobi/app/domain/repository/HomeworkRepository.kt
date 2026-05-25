package com.dobi.app.domain.repository

import com.dobi.app.domain.model.Homework

interface HomeworkRepository {
    /** Returns the list of remote photo URLs that were uploaded to Storage. */
    suspend fun saveHomework(
        homework: Homework,
        localPhotoUriStrings: List<String>
    ): List<String>
}
