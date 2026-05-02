package com.dobi.app.data.repository

import com.dobi.app.domain.model.FriendSnap
import com.dobi.app.domain.repository.FriendSnapsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub for now — emits an empty list so the Streak dashboard renders the
 * "your friends haven't shared any snap with you yet" empty state. When the
 * Firestore-backed feed lands, swap this implementation for a snapshot flow
 * scoped to the current child's friend graph.
 */
@Singleton
class FriendSnapsRepositoryImpl @Inject constructor() : FriendSnapsRepository {
    override fun snapsForCurrentChild(): Flow<List<FriendSnap>> = flowOf(emptyList())
}
