package com.dobi.app.data.repository

import com.dobi.app.domain.model.PairingCode
import com.dobi.app.domain.repository.PairingRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.datetime.Clock
import kotlin.random.Random

class PairingRepositoryImpl(
    private val firestore: FirebaseFirestore
) : PairingRepository {

    companion object {
        private const val COLLECTION = "pairing_codes"
        private const val EXPIRY_MS = 10 * 60 * 1000L
    }

    override suspend fun generateCode(parentId: String, childId: String): Result<PairingCode> = runCatching {
        val code = Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
        val now = Clock.System.now().toEpochMilliseconds()
        val pairing = PairingCode(
            code = code,
            parentId = parentId,
            childId = childId,
            createdAt = now,
            expiresAt = now + EXPIRY_MS,
            used = false
        )
        firestore.collection(COLLECTION).document(code).set(pairing.toMap())
        pairing
    }

    override suspend fun redeemCode(code: String): Result<PairingCode> = runCatching {
        val ref = firestore.collection(COLLECTION).document(code)
        val snapshot = ref.get()
        if (!snapshot.exists) error("Invalid code. Please check and try again.")

        val pairing = snapshot.toPairingCode()
        if (pairing.used) error("This code has already been used.")
        if (Clock.System.now().toEpochMilliseconds() > pairing.expiresAt) {
            error("This code has expired. Ask the parent to generate a new one.")
        }
        ref.update("used" to true)
        pairing
    }
}

private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toPairingCode() = PairingCode(
    code = get("code") as? String ?: "",
    parentId = get("parentId") as? String ?: "",
    childId = get("childId") as? String ?: "",
    createdAt = get("createdAt") as? Long ?: 0L,
    expiresAt = get("expiresAt") as? Long ?: 0L,
    used = get("used") as? Boolean ?: false
)

private fun PairingCode.toMap(): Map<String, Any?> = mapOf(
    "code" to code,
    "parentId" to parentId,
    "childId" to childId,
    "createdAt" to createdAt,
    "expiresAt" to expiresAt,
    "used" to used
)
