package com.dobi.app.domain.repository

import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskType
import kotlinx.coroutines.flow.Flow

interface ParentTaskRepository {
    /** Child sends a topic request to their parent. Returns the new doc id. */
    suspend fun requestFromChild(
        unitId: String,
        unitName: String,
        conceptId: String,
        conceptName: String,
        isUnitScope: Boolean,
        subject: String,
        subjectColor: String,
        subjectIcon: String
    ): Result<String>

    /** Parent assigns a task type (with optional note + mcq count) to a request. */
    suspend fun assignByParent(
        taskId: String,
        type: ParentTaskType,
        customNote: String,
        mcqCount: Int
    ): Result<Unit>

    /** Child submits artefacts (already-uploaded URLs) and optional quiz score. */
    suspend fun submitByChild(
        taskId: String,
        photoUrls: List<String>,
        score: Int?,
        total: Int?
    ): Result<Unit>

    /** Convenience: uploads local photos to Storage then calls [submitByChild]. */
    suspend fun submitWithLocalPhotos(
        taskId: String,
        localPhotoUris: List<String>
    ): Result<Unit>

    /** Parent accepts the submission. */
    suspend fun acceptByParent(taskId: String, reviewNote: String = ""): Result<Unit>

    /** Parent rejects, sends back to ASSIGNED for the child to redo. */
    suspend fun rejectByParent(taskId: String, reviewNote: String): Result<Unit>

    /** All tasks for the active child (any status). */
    fun observeForActiveChild(): Flow<List<ParentTask>>

    /** All tasks for the active parent across all their children. */
    fun observeForActiveParent(): Flow<List<ParentTask>>

    /** One-shot fetch by id. */
    suspend fun getById(taskId: String): Result<ParentTask>
}
