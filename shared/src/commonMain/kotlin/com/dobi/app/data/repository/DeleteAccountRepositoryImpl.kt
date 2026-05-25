package com.dobi.app.data.repository

import com.dobi.app.domain.repository.DeleteAccountRepository
import dev.gitlive.firebase.functions.FirebaseFunctions

class DeleteAccountRepositoryImpl(
    private val functions: FirebaseFunctions
) : DeleteAccountRepository {

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        functions.httpsCallable("deleteAccount").invoke(emptyMap<String, Any>())
        Unit
    }
}
