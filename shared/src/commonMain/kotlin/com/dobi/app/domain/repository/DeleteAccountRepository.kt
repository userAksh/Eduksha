package com.dobi.app.domain.repository

interface DeleteAccountRepository {
    /**
     * Calls the `deleteAccount` Cloud Function. Server-side wipes every
     * Firestore doc, Storage object, and the Auth user tied to the caller.
     * Returns Result.success on a clean wipe, Result.failure otherwise so
     * the UI can surface the error and let the user retry.
     */
    suspend fun deleteAccount(): Result<Unit>
}
