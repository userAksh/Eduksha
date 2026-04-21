package com.dobi.app.data.repository

import com.dobi.app.domain.model.PairingCode
import com.dobi.app.domain.repository.PairingRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class PairingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PairingRepository {

    companion object {
        private const val COLLECTION = "pairing_codes"
        private const val EXPIRY_MS = 10 * 60 * 1000L // 10 minutes
    }

    override suspend fun generateCode(parentId: String, childId: String): Result<PairingCode> {
        return try {
            val code = Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
            val now = System.currentTimeMillis()
            val pairing = PairingCode(
                code = code,
                parentId = parentId,
                childId = childId,
                createdAt = now,
                expiresAt = now + EXPIRY_MS,
                used = false
            )
            firestore.collection(COLLECTION).document(code).set(pairing).await()
            Result.success(pairing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun redeemCode(code: String): Result<PairingCode> {
        return try {
            val ref = firestore.collection(COLLECTION).document(code)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalStateException("Invalid code. Please check and try again."))
            }
            val pairing = snapshot.toObject(PairingCode::class.java)
                ?: return Result.failure(IllegalStateException("Invalid pairing record."))

            if (pairing.used) {
                return Result.failure(IllegalStateException("This code has already been used."))
            }
            if (System.currentTimeMillis() > pairing.expiresAt) {
                return Result.failure(IllegalStateException("This code has expired. Ask the parent to generate a new one."))
            }

            ref.update("used", true).await()
            Result.success(pairing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
