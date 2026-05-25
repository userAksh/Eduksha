package com.dobi.app.domain.repository

import com.dobi.app.domain.model.PairingCode

interface PairingRepository {
    suspend fun generateCode(parentId: String, childId: String): Result<PairingCode>
    suspend fun redeemCode(code: String): Result<PairingCode>
}
