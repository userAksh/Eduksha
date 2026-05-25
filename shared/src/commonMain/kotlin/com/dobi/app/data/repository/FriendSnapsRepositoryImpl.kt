package com.dobi.app.data.repository

import com.dobi.app.domain.model.FriendSnap
import com.dobi.app.domain.repository.FriendSnapsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FriendSnapsRepositoryImpl : FriendSnapsRepository {
    override fun snapsForCurrentChild(): Flow<List<FriendSnap>> = flowOf(emptyList())
}
