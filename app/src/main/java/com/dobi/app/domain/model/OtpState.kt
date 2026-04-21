package com.dobi.app.domain.model

import com.google.firebase.auth.FirebaseUser

sealed class OtpState {
    object Sending : OtpState()
    data class CodeSent(val verificationId: String) : OtpState()
    data class AutoVerified(val user: FirebaseUser) : OtpState()
    data class Error(val message: String) : OtpState()
}
