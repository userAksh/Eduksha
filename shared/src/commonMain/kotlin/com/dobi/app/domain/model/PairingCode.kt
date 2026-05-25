package com.dobi.app.domain.model

data class PairingCode(
    val code: String = "",
    val parentId: String = "",
    val childId: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val used: Boolean = false
)
