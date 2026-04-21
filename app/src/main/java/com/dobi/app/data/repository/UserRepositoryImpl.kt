package com.dobi.app.data.repository

import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.model.UserRole
import com.dobi.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userPreferences: UserPreferences
) : UserRepository {

    override val userRole: Flow<UserRole?> = userPreferences.userRole

    override suspend fun selectRole(role: UserRole) = userPreferences.saveRole(role)
}
