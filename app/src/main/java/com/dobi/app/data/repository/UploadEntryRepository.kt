package com.dobi.app.data.repository

import android.net.Uri
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.model.UploadEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Persists child uploads to Firestore + Storage so they can be browsed later
 * on the Revisit Past Topics flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class UploadEntryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val userPreferences: UserPreferences
) {

    /**
     * Uploads each photo to Storage then writes a single Firestore doc with
     * the resulting URLs. Returns the saved entry on success.
     */
    suspend fun save(entry: UploadEntry, localPhotoUris: List<String>): Result<UploadEntry> =
        runCatching {
            val childId = resolveChildId(entry.childId)
            val urls = localPhotoUris.map { uri -> uploadPhoto(childId, uri) }
            writeEntry(entry, childId, urls)
        }

    /**
     * Writes the Firestore doc using already-uploaded photo URLs. Lets callers
     * that uploaded the photos elsewhere (e.g. HomeworkLog → homework_photos/)
     * surface the same content on the Revisit Past Topics flow without
     * uploading bytes a second time.
     */
    suspend fun saveWithUrls(entry: UploadEntry, photoUrls: List<String>): Result<UploadEntry> =
        runCatching {
            val childId = resolveChildId(entry.childId)
            writeEntry(entry, childId, photoUrls)
        }

    private suspend fun resolveChildId(provided: String): String {
        val resolved = provided.ifBlank { userPreferences.linkedChildId.first().orEmpty() }
        require(resolved.isNotBlank()) { "No active child id" }
        return resolved
    }

    private suspend fun writeEntry(
        entry: UploadEntry,
        childId: String,
        photoUrls: List<String>
    ): UploadEntry {
        val docRef = firestore.collection("uploads").document()
        val saved = entry.copy(
            id = docRef.id,
            childId = childId,
            photoUrls = photoUrls,
            timestamp = if (entry.timestamp > 0) entry.timestamp else System.currentTimeMillis()
        )
        val data = mapOf(
            "id" to saved.id,
            "childId" to saved.childId,
            "kind" to saved.kind,
            "subject" to saved.subject,
            "subjectColor" to saved.subjectColor,
            "subjectIcon" to saved.subjectIcon,
            "unitId" to saved.unitId,
            "unitName" to saved.unitName,
            "conceptId" to saved.conceptId,
            "conceptName" to saved.conceptName,
            "topic" to saved.topic,
            "photoUrls" to saved.photoUrls,
            "timestamp" to saved.timestamp,
            "createdAt" to Timestamp.now()
        )
        suspendCancellableCoroutine<Unit> { cont ->
            docRef.set(data)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        return saved
    }

    /**
     * Streams all uploads for the active child, ordered newest first.
     * Empty list when there's no active child yet.
     */
    fun observeForActiveChild(): Flow<List<UploadEntry>> =
        userPreferences.linkedChildId.flatMapLatest { childId ->
            if (childId.isNullOrBlank()) flowOf(emptyList())
            else observeForChild(childId)
        }

    private fun observeForChild(childId: String): Flow<List<UploadEntry>> = callbackFlow {
        // Note: we sort client-side rather than .orderBy("timestamp") so that the
        // server-side query is a simple equality filter and avoids needing a
        // composite Firestore index (childId + timestamp DESC).
        val listener = firestore.collection("uploads")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        UploadEntry(
                            id = doc.getString("id").orEmpty(),
                            childId = doc.getString("childId").orEmpty(),
                            kind = doc.getString("kind").orEmpty(),
                            subject = doc.getString("subject").orEmpty(),
                            subjectColor = doc.getString("subjectColor").orEmpty(),
                            subjectIcon = doc.getString("subjectIcon").orEmpty(),
                            unitId = doc.getString("unitId").orEmpty(),
                            unitName = doc.getString("unitName").orEmpty(),
                            conceptId = doc.getString("conceptId").orEmpty(),
                            conceptName = doc.getString("conceptName").orEmpty(),
                            topic = doc.getString("topic").orEmpty(),
                            photoUrls = (doc.get("photoUrls") as? List<*>).orEmpty()
                                .filterIsInstance<String>(),
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }.getOrNull()
                }.sortedByDescending { it.timestamp }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    private suspend fun uploadPhoto(childId: String, localUri: String): String {
        val ref = storage.reference.child("uploads/$childId/${UUID.randomUUID()}.jpg")
        // Chain put -> downloadUrl in a single Task pipeline. Calling
        // ref.downloadUrl directly after putFile().addOnSuccessListener can
        // race against Storage's metadata commit and surface as
        // "Object does not exist at location" (a 404). continueWithTask
        // guarantees the upload is fully finalised before the URL fetch.
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

}
