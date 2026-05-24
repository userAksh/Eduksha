package com.dobi.app.data.repository

import android.util.Log
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.repository.FcmTokenRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FcmTokenRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userPreferences: UserPreferences
) : FcmTokenRepository {

    // syncCurrentToken() is called from MainActivity, ParentHomeViewModel, and
    // ChildHomeViewModel on every startup. Token rarely rotates mid-session,
    // so skip redundant FCM fetches + Firestore writes after the first success.
    @Volatile private var syncedThisSession = false

    override suspend fun syncCurrentToken() {
        if (syncedThisSession) return
        syncedThisSession = true
        runCatching {
            val token = fetchToken()
            writeToken(token)
        }.onFailure {
            syncedThisSession = false  // allow retry on next call
            Log.w(TAG, "syncCurrentToken failed", it)
        }
    }

    override suspend fun onTokenRotated(token: String) = runCatching {
        writeToken(token)
    }.onFailure { Log.w(TAG, "onTokenRotated failed", it) }.let { Unit }

    private suspend fun writeToken(token: String) {
        val parentUid = auth.currentUser?.uid
        val activeChildId = userPreferences.linkedChildId.first()
        val linkedParentId = userPreferences.linkedParentId.first()

        // Parent device: write to parents/{uid}.fcmToken
        if (parentUid != null) {
            await {
                firestore.collection("parents").document(parentUid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
            }
        }

        // Child device: write to parents/{linkedParentId}/children/{childId}.fcmToken
        // The active child is the one currently signed in on the device.
        if (!activeChildId.isNullOrBlank() && !linkedParentId.isNullOrBlank()) {
            await {
                firestore.collection("parents").document(linkedParentId)
                    .collection("children").document(activeChildId)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
            }
        }
    }

    private suspend fun fetchToken(): String =
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    private suspend fun await(block: () -> com.google.android.gms.tasks.Task<Void>) {
        suspendCancellableCoroutine<Unit> { cont ->
            block()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    companion object {
        private const val TAG = "FcmTokenRepository"
    }
}
