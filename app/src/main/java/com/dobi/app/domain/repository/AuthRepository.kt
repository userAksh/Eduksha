package com.dobi.app.domain.repository

import android.app.Activity
import com.dobi.app.domain.model.OtpState
import com.dobi.app.domain.model.ParentProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: FirebaseUser?
    fun sendOtp(phone: String, activity: Activity): Flow<OtpState>
    suspend fun verifyOtp(verificationId: String, otp: String): Result<FirebaseUser>
    suspend fun saveParentProfile(name: String, phone: String)
    suspend fun getParentProfile(): ParentProfile?
    fun signOut()
}
