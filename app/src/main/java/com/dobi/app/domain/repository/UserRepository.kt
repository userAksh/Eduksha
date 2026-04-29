package com.dobi.app.domain.repository

import com.dobi.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val userRole: Flow<UserRole?>
    val linkedChildId: Flow<String?>
    val linkedParentId: Flow<String?>
    val linkedChildGrade: Flow<Int>
    suspend fun selectRole(role: UserRole)
    suspend fun saveLinkedChild(parentId: String, childId: String)
    suspend fun saveLinkedChildGrade(grade: Int)
}
