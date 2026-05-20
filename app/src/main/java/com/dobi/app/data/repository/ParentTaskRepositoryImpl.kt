package com.dobi.app.data.repository

import android.net.Uri
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.model.McqQuestion
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ParentTaskStatus
import com.dobi.app.domain.model.ParentTaskType
import com.dobi.app.domain.model.SubjectiveQuestion
import com.dobi.app.domain.repository.ParentTaskRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ParentTaskRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val userPreferences: UserPreferences
) : ParentTaskRepository {

    private val collection get() = firestore.collection(COLLECTION)

    override suspend fun requestFromChild(
        unitId: String,
        unitName: String,
        conceptId: String,
        conceptName: String,
        isUnitScope: Boolean,
        subject: String,
        subjectColor: String,
        subjectIcon: String
    ): Result<String> = runCatching {
        val childId = userPreferences.linkedChildId.first().orEmpty()
        val parentId = userPreferences.linkedParentId.first().orEmpty()
        require(childId.isNotBlank()) { "No active child id" }
        require(parentId.isNotBlank()) { "No linked parent id" }

        val docRef = collection.document()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "id" to docRef.id,
            "parentId" to parentId,
            "childId" to childId,
            "unitId" to unitId,
            "unitName" to unitName,
            "conceptId" to conceptId,
            "conceptName" to conceptName,
            "isUnitScope" to isUnitScope,
            "subject" to subject,
            "subjectColor" to subjectColor,
            "subjectIcon" to subjectIcon,
            "status" to ParentTaskStatus.REQUESTED.name,
            "requestedAtMillis" to now
        )
        awaitTask { docRef.set(data) }
        docRef.id
    }

    override suspend fun assignByParent(
        taskId: String,
        type: ParentTaskType,
        customNote: String,
        mcqCount: Int
    ): Result<Unit> = runCatching {
        val updates = mapOf(
            "type" to type.name,
            "customNote" to customNote,
            "mcqCount" to mcqCount,
            "status" to ParentTaskStatus.ASSIGNED.name,
            "assignedAtMillis" to System.currentTimeMillis()
        )
        awaitTask { collection.document(taskId).update(updates) }
    }

    override suspend fun assignDirectlyByParent(
        parentId: String,
        childId: String,
        unitId: String,
        unitName: String,
        conceptId: String,
        conceptName: String,
        isUnitScope: Boolean,
        subject: String,
        subjectColor: String,
        subjectIcon: String,
        type: ParentTaskType,
        customNote: String,
        mcqCount: Int,
        assignedMcqs: List<McqQuestion>,
        assignedSubjectiveQs: List<SubjectiveQuestion>,
    ): Result<String> = runCatching {
        require(parentId.isNotBlank()) { "Missing parent id" }
        require(childId.isNotBlank()) { "Missing child id" }
        val docRef = collection.document()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "id" to docRef.id,
            "parentId" to parentId,
            "childId" to childId,
            "unitId" to unitId,
            "unitName" to unitName,
            "conceptId" to conceptId,
            "conceptName" to conceptName,
            "isUnitScope" to isUnitScope,
            "subject" to subject,
            "subjectColor" to subjectColor,
            "subjectIcon" to subjectIcon,
            "type" to type.name,
            "customNote" to customNote,
            "mcqCount" to mcqCount,
            "assignedMcqs" to assignedMcqs.map(::mcqToMap),
            "assignedSubjectiveQs" to assignedSubjectiveQs.map(::subjectiveToMap),
            "status" to ParentTaskStatus.ASSIGNED.name,
            "requestedAtMillis" to now,
            "assignedAtMillis" to now,
        )
        awaitTask { docRef.set(data) }
        docRef.id
    }

    override suspend fun sendReminder(taskId: String): Result<Unit> = runCatching {
        awaitTask {
            collection.document(taskId).update(
                mapOf("reminderSentAtMillis" to System.currentTimeMillis())
            )
        }
    }

    private fun mcqToMap(q: McqQuestion): Map<String, Any?> = mapOf(
        "id" to q.id,
        "text" to q.text,
        "options" to q.options,
        "correctIndex" to q.correctIndex,
        "explanation" to q.explanation,
    )

    private fun subjectiveToMap(q: SubjectiveQuestion): Map<String, Any?> = mapOf(
        "id" to q.id,
        "text" to q.text,
        "expectedKeywords" to q.expectedKeywords,
        "maxMarks" to q.maxMarks,
    )

    override suspend fun submitByChild(
        taskId: String,
        photoUrls: List<String>,
        score: Int?,
        total: Int?
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any?>(
            "submissionPhotoUrls" to photoUrls,
            "status" to ParentTaskStatus.PENDING_REVIEW.name,
            "submittedAtMillis" to System.currentTimeMillis()
        )
        if (score != null) updates["submissionScore"] = score
        if (total != null) updates["submissionTotal"] = total
        awaitTask { collection.document(taskId).update(updates) }
    }

    override suspend fun submitWithLocalPhotos(
        taskId: String,
        localPhotoUris: List<String>
    ): Result<Unit> = runCatching {
        val childId = userPreferences.linkedChildId.first().orEmpty()
        require(childId.isNotBlank()) { "No active child id" }
        val urls = localPhotoUris.map { uri -> uploadPhoto(childId, taskId, uri) }
        submitByChild(taskId, urls, score = null, total = null).getOrThrow()
    }

    private suspend fun uploadPhoto(
        childId: String,
        taskId: String,
        localUri: String
    ): String {
        // Match the existing UploadEntryRepository path shape exactly —
        // `uploads/<childId>/<single-filename>` — so the Storage rule that
        // already authorizes self-learning uploads applies here too.
        // taskId is folded into the filename for traceability.
        val ref = storage.reference.child(
            "uploads/$childId/parenttask_${taskId}_${UUID.randomUUID()}.jpg"
        )
        return suspendCancellableCoroutine { cont ->
            ref.putFile(Uri.parse(localUri))
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception
                        ?: IllegalStateException("Upload failed")
                    ref.downloadUrl
                }
                .addOnSuccessListener { cont.resume(it.toString()) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    override suspend fun acceptByParent(taskId: String, reviewNote: String): Result<Unit> =
        runCatching {
            awaitTask {
                collection.document(taskId).update(
                    mapOf(
                        "status" to ParentTaskStatus.ACCEPTED.name,
                        "parentReviewNote" to reviewNote,
                        "reviewedAtMillis" to System.currentTimeMillis()
                    )
                )
            }
        }

    override suspend fun rejectByParent(taskId: String, reviewNote: String): Result<Unit> =
        runCatching {
            // Sends back to ASSIGNED so the child can redo. Wipes prior
            // submission artefacts so the redo is a clean attempt.
            awaitTask {
                collection.document(taskId).update(
                    mapOf(
                        "status" to ParentTaskStatus.REJECTED.name,
                        "parentReviewNote" to reviewNote,
                        "reviewedAtMillis" to System.currentTimeMillis(),
                        "submissionPhotoUrls" to emptyList<String>(),
                        "submissionScore" to null,
                        "submissionTotal" to null
                    )
                )
            }
        }

    override fun observeForActiveChild(): Flow<List<ParentTask>> =
        userPreferences.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank()) flowOf(emptyList())
            else observeBy("childId", childId)
        }

    override fun observeForActiveParent(): Flow<List<ParentTask>> {
        val parentId = auth.currentUser?.uid ?: return flowOf(emptyList())
        return observeBy("parentId", parentId)
    }

    private fun observeBy(field: String, value: String): Flow<List<ParentTask>> = callbackFlow {
        val listener = collection
            .whereEqualTo(field, value)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // Don't propagate via close(error) — that lets the
                    // exception bubble up viewModelScope on Dispatchers.Main
                    // and crashes the app on PERMISSION_DENIED when Firestore
                    // rules reject this listener (e.g. signed-out user with a
                    // stale linkedChildId). Emit empty and complete cleanly so
                    // the UI shows an empty list instead of crashing.
                    trySend(emptyList()); close(); return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty()
                    .mapNotNull { it.toParentTask() }
                    .sortedByDescending {
                        // Show newest activity first regardless of which
                        // milestone advanced last.
                        maxOf(
                            it.requestedAtMillis,
                            it.assignedAtMillis,
                            it.submittedAtMillis,
                            it.reviewedAtMillis
                        )
                    }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getById(taskId: String): Result<ParentTask> = runCatching {
        val snap = suspendCancellableCoroutine { cont ->
            collection.document(taskId).get()
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        snap.toParentTask() ?: error("Task not found")
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toParentTask(): ParentTask? =
        runCatching {
            ParentTask(
                id = getString("id").orEmpty(),
                parentId = getString("parentId").orEmpty(),
                childId = getString("childId").orEmpty(),
                unitId = getString("unitId").orEmpty(),
                unitName = getString("unitName").orEmpty(),
                conceptId = getString("conceptId").orEmpty(),
                conceptName = getString("conceptName").orEmpty(),
                isUnitScope = getBoolean("isUnitScope") ?: true,
                subject = getString("subject").orEmpty(),
                subjectColor = getString("subjectColor").orEmpty(),
                subjectIcon = getString("subjectIcon").orEmpty(),
                type = getString("type")?.let {
                    runCatching { ParentTaskType.valueOf(it) }.getOrNull()
                },
                customNote = getString("customNote").orEmpty(),
                mcqCount = (getLong("mcqCount") ?: 5L).toInt(),
                assignedMcqs = (get("assignedMcqs") as? List<*>)
                    .orEmpty().mapNotNull(::mapToMcq),
                assignedSubjectiveQs = (get("assignedSubjectiveQs") as? List<*>)
                    .orEmpty().mapNotNull(::mapToSubjective),
                submissionPhotoUrls = (get("submissionPhotoUrls") as? List<*>)
                    .orEmpty().filterIsInstance<String>(),
                submissionScore = getLong("submissionScore")?.toInt(),
                submissionTotal = getLong("submissionTotal")?.toInt(),
                parentReviewNote = getString("parentReviewNote").orEmpty(),
                status = getString("status")?.let {
                    runCatching { ParentTaskStatus.valueOf(it) }.getOrNull()
                } ?: ParentTaskStatus.REQUESTED,
                requestedAtMillis = getLong("requestedAtMillis") ?: 0L,
                assignedAtMillis = getLong("assignedAtMillis") ?: 0L,
                submittedAtMillis = getLong("submittedAtMillis") ?: 0L,
                reviewedAtMillis = getLong("reviewedAtMillis") ?: 0L,
                reminderSentAtMillis = getLong("reminderSentAtMillis") ?: 0L,
            )
        }.getOrNull()

    private fun mapToMcq(raw: Any?): McqQuestion? {
        val m = raw as? Map<*, *> ?: return null
        return McqQuestion(
            id = (m["id"] as? String).orEmpty(),
            text = (m["text"] as? String).orEmpty(),
            options = (m["options"] as? List<*>).orEmpty().filterIsInstance<String>(),
            correctIndex = ((m["correctIndex"] as? Number)?.toInt()) ?: 0,
            explanation = (m["explanation"] as? String).orEmpty(),
        ).takeIf { it.options.isNotEmpty() }
    }

    private fun mapToSubjective(raw: Any?): SubjectiveQuestion? {
        val m = raw as? Map<*, *> ?: return null
        return SubjectiveQuestion(
            id = (m["id"] as? String).orEmpty(),
            text = (m["text"] as? String).orEmpty(),
            expectedKeywords = (m["expectedKeywords"] as? List<*>).orEmpty()
                .filterIsInstance<String>(),
            maxMarks = ((m["maxMarks"] as? Number)?.toInt()) ?: 2,
        ).takeIf { it.text.isNotBlank() }
    }

    private suspend fun awaitTask(block: () -> com.google.android.gms.tasks.Task<Void>) {
        suspendCancellableCoroutine<Unit> { cont ->
            block()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    companion object {
        private const val COLLECTION = "parentTaskRequests"
    }
}
