package com.dobi.app.data.repository

import android.net.Uri
import com.dobi.app.domain.model.Homework
import com.dobi.app.domain.repository.HomeworkRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class HomeworkRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : HomeworkRepository {

    override suspend fun saveHomework(
        homework: Homework,
        localPhotoUriStrings: List<String>
    ): List<String> {
        val photoUrls = localPhotoUriStrings.map { uriString ->
            val ref = storage.reference
                .child("homework_photos/${homework.childId}/${UUID.randomUUID()}.jpg")
            // continueWithTask chains the upload -> downloadUrl on the same
            // Task graph. Issuing downloadUrl in a separate suspend block
            // after the upload's success listener can race against Storage's
            // metadata commit and surface as "Object does not exist at
            // location" (a 404).
            suspendCancellableCoroutine<String> { cont ->
                ref.putFile(Uri.parse(uriString))
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception
                            ?: IllegalStateException("Upload failed")
                        ref.downloadUrl
                    }
                    .addOnSuccessListener { cont.resume(it.toString()) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        }

        val data = hashMapOf(
            "childId"     to homework.childId,
            "subject"     to homework.subject,
            "teacherName" to homework.teacherName,
            "chapterName" to homework.chapterName,
            "date"        to homework.date,
            "photoUrls"   to photoUrls,
            "timestamp"   to Timestamp.now()
        )
        suspendCancellableCoroutine { cont ->
            firestore.collection("homeworks").add(data)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        return photoUrls
    }
}
