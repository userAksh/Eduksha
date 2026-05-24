package com.dobi.app.data.repository

import android.app.Activity
import android.util.Log
import com.dobi.app.domain.model.OtpState
import com.dobi.app.domain.model.ParentProfile
import com.dobi.app.domain.repository.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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

    // Retained across calls so resend can bypass Firebase's same-number throttle.
    private var lastResendToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun sendOtp(phone: String, activity: Activity): Flow<OtpState> = callbackFlow {
        Log.d("AuthRepo", "sendOtp: phone=$phone")
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
                Log.e("AuthRepo", "onVerificationFailed: class=${e.javaClass.simpleName} message=${e.message}")
                trySend(OtpState.Error(e.toFriendlyMessage()))
                close()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                lastResendToken = token
                trySend(OtpState.CodeSent(verificationId))
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        lastResendToken?.let { builder.setForceResendingToken(it) }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())

        awaitClose()
    }

    override suspend fun verifyOtp(verificationId: String, otp: String): Result<FirebaseUser> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e("AuthRepo", "verifyOtp failed: class=${e.javaClass.simpleName} message=${e.message}")
            Result.failure(Exception(e.toFriendlyMessage()))
        }
    }

    override suspend fun saveParentProfile(name: String, phone: String) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = firestore.collection("parents").document(uid)
        val existing = docRef.get().await()
        if (existing.exists()) return
        docRef.set(mapOf("name" to name, "phone" to phone)).await()
    }

    override suspend fun getParentProfile(): ParentProfile? {
        val uid = auth.currentUser?.uid ?: return null
        val ref = firestore.collection("parents").document(uid)
        // Serve from Firestore's local disk cache first (returns instantly on
        // subsequent launches). Falls back to the server on first launch or
        // when the cache has been evicted.
        val cached = runCatching { ref.get(Source.CACHE).await() }.getOrNull()
        val doc = if (cached?.exists() == true) cached else ref.get().await()
        return if (doc.exists()) {
            ParentProfile(
                uid = uid,
                name = doc.getString("name") ?: "",
                phone = doc.getString("phone") ?: ""
            )
        } else null
    }

    override fun signOut() = auth.signOut()
}

private fun Exception.toFriendlyMessage(): String {
    val code = message?.uppercase() ?: ""
    return when {
        this is FirebaseTooManyRequestsException ||
        code.contains("TOO_MANY_REQUESTS") || code.contains("QUOTA_EXCEEDED") ->
            "Too many attempts. Please wait a few minutes and try again."
        code.contains("NETWORK_ERROR") || code.contains("NETWORK_REQUEST_FAILED") ->
            "No internet connection. Please check your network and try again."
        this is FirebaseAuthInvalidCredentialsException || code.contains("INVALID_CREDENTIALS") -> {
            when {
                code.contains("INVALID_PHONE_NUMBER") ||
                code.contains("TOO_LONG") || code.contains("TOO_SHORT") ->
                    "Please enter a valid 10-digit mobile number."
                code.contains("INVALID_CODE") || code.contains("INVALID_VERIFICATION_CODE") ->
                    "Incorrect OTP. Please check the code and try again."
                code.contains("SESSION_EXPIRED") ->
                    "OTP has expired. Tap 'Resend OTP' to get a new code."
                else -> "Verification failed. Please try again."
            }
        }
        code.contains("SESSION_EXPIRED") ->
            "OTP has expired. Tap 'Resend OTP' to get a new code."
        code.contains("INVALID_CODE") || code.contains("INVALID_VERIFICATION_CODE") ->
            "Incorrect OTP. Please check the code and try again."
        else -> "Something went wrong. Please try again."
    }
}
