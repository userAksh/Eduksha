package com.dobi.app.domain.repository

import com.dobi.app.domain.model.Child
import kotlinx.coroutines.flow.Flow

interface ChildRepository {
    fun getChildren(parentId: String): Flow<List<Child>>
    suspend fun addChild(child: Child): Result<Child>
}
