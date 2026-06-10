package com.dobi.app.data.repository

import android.content.Context
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.dobi.app.domain.model.ReferralReward
import com.dobi.app.domain.repository.ClaimOutcome
import com.dobi.app.domain.repository.InviteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class InviteRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
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

    override suspend fun createParentInviteShareUrl(): Result<Pair<String, String>> = runCatching {
        val result = await { functions.getHttpsCallable("createInvite").call(mapOf("inviterType" to "parent")) }
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?> ?: error("createInvite returned no payload")
        val code = (payload["code"] as? String).orEmpty().ifBlank { error("blank code") }
        playStoreShareUrl(code) to code
    }.onFailure { Log.w(TAG, "createParentInvite failed", it) }

    override suspend fun claimParentInvite(code: String): Result<ClaimOutcome> = runCatching {
        val result = await { functions.getHttpsCallable("claimInvite").call(mapOf("code" to code)) }
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?>
        when ((payload?.get("status") as? String).orEmpty()) {
            "already-claimed" -> ClaimOutcome.ALREADY_CLAIMED
            "self"            -> ClaimOutcome.SELF_INVITE
            else              -> ClaimOutcome.CLAIMED
        }
    }.onFailure { Log.w(TAG, "claimParentInvite failed for code=$code", it) }

    override suspend fun applyReferralMatch(childId: String, grade: Int, board: String): Result<Unit> =
        runCatching {
            await {
                functions.getHttpsCallable("applyReferralMatch")
                    .call(mapOf("childId" to childId, "grade" to grade, "board" to board))
            }
            Unit
        }.onFailure { Log.w(TAG, "applyReferralMatch failed", it) }

    override suspend fun recordReferralWeekActivity(): Result<Unit> = runCatching {
        await { functions.getHttpsCallable("recordReferralWeekActivity").call(null) }
        Unit
    }.onFailure { Log.w(TAG, "recordReferralWeekActivity failed", it) }

    override fun observeReferralRewards(): Flow<List<ReferralReward>> {
        val parentId = auth.currentUser?.uid ?: return flowOf(emptyList())
        return callbackFlow {
            var reg: ListenerRegistration? = null
            reg = firestore.collection("parents").document(parentId)
                .collection("referralRewards")
                .orderBy("unlockedAtMillis")
                .addSnapshotListener { snap, err ->
                    if (err != null) { Log.w(TAG, "referralRewards listener error", err); return@addSnapshotListener }
                    val rewards = snap?.documents?.mapNotNull { doc ->
                        runCatching {
                            ReferralReward(
                                id                = doc.id,
                                stage             = (doc.getLong("stage") ?: 0L).toInt(),
                                referredParentId  = doc.getString("referredParentId").orEmpty(),
                                referredParentName= doc.getString("referredParentName").orEmpty(),
                                unlockedAtMillis  = doc.getLong("unlockedAtMillis") ?: 0L,
                                scratchedAtMillis = doc.getLong("scratchedAtMillis") ?: 0L,
                                rewardLabel       = doc.getString("rewardLabel").orEmpty(),
                                rewardValue       = doc.getString("rewardValue").orEmpty(),
                            )
                        }.getOrNull()
                    } ?: emptyList()
                    trySend(rewards)
                }
            awaitClose { reg?.remove() }
        }
    }

    override suspend fun scratchReward(rewardId: String): Result<String> = runCatching {
        val result = await {
            functions.getHttpsCallable("scratchReward").call(mapOf("rewardId" to rewardId))
        }
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?>
        (payload?.get("rewardValue") as? String).orEmpty()
    }.onFailure { Log.w(TAG, "scratchReward failed for id=$rewardId", it) }

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
        private const val APP_ID = "in.eduksha.app"

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
