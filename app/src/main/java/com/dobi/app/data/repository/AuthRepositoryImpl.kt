package com.dobi.app.data.repository

import android.app.Activity
import com.dobi.app.domain.model.OtpState
import com.dobi.app.domain.model.ParentProfile
import com.dobi.app.domain.repository.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: FirebaseUser? get() = auth.currentUser

    override fun sendOtp(phone: String, activity: Activity): Flow<OtpState> = callbackFlow {
        trySend(OtpState.Sending)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.user?.let { trySend(OtpState.AutoVerified(it)) }
                    } else {
                        trySend(OtpState.Error(task.exception?.message ?: "Auto-verification failed"))
                    }
                    close()
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                trySend(OtpState.Error(e.message ?: "Verification failed"))
                close()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                trySend(OtpState.CodeSent(verificationId))
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

        awaitClose()
    }

    override suspend fun verifyOtp(verificationId: String, otp: String): Result<FirebaseUser> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveParentProfile(name: String, phone: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("parents").document(uid)
            .set(mapOf("name" to name, "phone" to phone))
            .await()
    }

    override suspend fun getParentProfile(): ParentProfile? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = firestore.collection("parents").document(uid).get().await()
            if (doc.exists()) {
                ParentProfile(
                    uid = uid,
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: ""
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override fun signOut() = auth.signOut()
}
