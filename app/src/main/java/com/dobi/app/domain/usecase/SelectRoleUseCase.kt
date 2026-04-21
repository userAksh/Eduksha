package com.dobi.app.domain.usecase

import com.dobi.app.domain.model.UserRole
import com.dobi.app.domain.repository.UserRepository
import javax.inject.Inject

class SelectRoleUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(role: UserRole) = repository.selectRole(role)
}
