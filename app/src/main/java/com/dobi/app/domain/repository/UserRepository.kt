package com.dobi.app.domain.repository

import com.dobi.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val userRole: Flow<UserRole?>
    suspend fun selectRole(role: UserRole)
}
