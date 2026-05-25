package com.dobi.app.domain.usecase

import com.dobi.app.domain.model.UserRole
import com.dobi.app.domain.repository.UserRepository

class SelectRoleUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(role: UserRole) = repository.selectRole(role)
}
