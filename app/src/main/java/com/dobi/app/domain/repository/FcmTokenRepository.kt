package com.dobi.app.domain.repository

interface FcmTokenRepository {
    /**
     * Persists the current FCM token to Firestore for the active role.
     * No-op if no active session yet.
     */
    suspend fun syncCurrentToken()

    /**
     * Called from FirebaseMessagingService when the SDK rotates the token.
     */
    suspend fun onTokenRotated(token: String)
}
