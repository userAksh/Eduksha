package com.dobi.app.data.repository

import android.util.Log
import com.dobi.app.domain.repository.DeleteAccountRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class DeleteAccountRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions
) : DeleteAccountRepository {

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        await {
            functions.getHttpsCallable("deleteAccount").call(emptyMap<String, Any>())
        }
        Unit
    }.onFailure { Log.w(TAG, "deleteAccount failed", it) }

    private suspend fun <T> await(
        block: () -> com.google.android.gms.tasks.Task<T>
    ): T = suspendCancellableCoroutine { cont ->
        block()
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    companion object {
        private const val TAG = "DeleteAccountRepository"
    }
}
