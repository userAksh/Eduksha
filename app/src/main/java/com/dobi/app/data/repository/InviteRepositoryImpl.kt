package com.dobi.app.data.repository

import android.content.Context
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.dobi.app.domain.repository.ClaimOutcome
import com.dobi.app.domain.repository.InviteRepository
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class InviteRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val functions: FirebaseFunctions
) : InviteRepository {

    override suspend fun createInviteShareUrl(childId: String): Result<String> = runCatching {
        val data = mapOf("childId" to childId)
        val result = await {
            functions.getHttpsCallable("createInvite").call(data)
        }
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?>
            ?: error("createInvite returned no payload")
        val code = (payload["code"] as? String).orEmpty()
        if (code.isBlank()) error("createInvite returned blank code")
        playStoreShareUrl(code)
    }.onFailure { Log.w(TAG, "createInvite failed", it) }

    override suspend fun claimInvite(code: String, childId: String): Result<ClaimOutcome> =
        runCatching {
            val data = mapOf("code" to code, "childId" to childId)
            val result = await {
                functions.getHttpsCallable("claimInvite").call(data)
            }
            @Suppress("UNCHECKED_CAST")
            val payload = result.data as? Map<String, Any?>
            val status = (payload?.get("status") as? String).orEmpty()
            when (status) {
                "claimed" -> ClaimOutcome.CLAIMED
                "already-claimed" -> ClaimOutcome.ALREADY_CLAIMED
                "self" -> ClaimOutcome.SELF_INVITE
                "inviter-missing" -> ClaimOutcome.INVITER_MISSING
                else -> ClaimOutcome.CLAIMED
            }
        }.onFailure { Log.w(TAG, "claimInvite failed for code=$code", it) }

    override suspend fun readPendingInviteCode(): String? {
        return runCatching { readReferrer() }
            .onFailure { Log.w(TAG, "InstallReferrer read failed", it) }
            .getOrNull()
            ?.let(::parseInviteCode)
    }

    private suspend fun readReferrer(): String? = suspendCancellableCoroutine { cont ->
        val client = InstallReferrerClient.newBuilder(context).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                val referrer = if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    runCatching { client.installReferrer.installReferrer }.getOrNull()
                } else null
                runCatching { client.endConnection() }
                if (cont.isActive) cont.resume(referrer)
            }

            override fun onInstallReferrerServiceDisconnected() { /* ignored */ }
        })
        cont.invokeOnCancellation { runCatching { client.endConnection() } }
    }

    private suspend fun <T> await(
        block: () -> com.google.android.gms.tasks.Task<T>
    ): T = suspendCancellableCoroutine { cont ->
        block()
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    companion object {
        private const val TAG = "InviteRepository"
        private const val INVITE_PREFIX = "invite_"
        private const val APP_ID = "com.dobi.app"

        fun parseInviteCode(referrer: String?): String? {
            if (referrer.isNullOrBlank()) return null
            // Play passes the referrer either as the raw query string we set
            // (`invite_abc123`) or url-encoded inside a key like `utm_source=`.
            // We accept either by scanning every '&'/'=' delimited token.
            return referrer.split('&', '=', '?')
                .firstOrNull { it.startsWith(INVITE_PREFIX) }
                ?.removePrefix(INVITE_PREFIX)
                ?.takeIf { it.isNotBlank() }
        }

        fun playStoreShareUrl(code: String): String =
            "https://play.google.com/store/apps/details?id=$APP_ID&referrer=$INVITE_PREFIX$code"
    }
}
