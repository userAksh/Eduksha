package com.dobi.app.domain.repository

import com.dobi.app.domain.model.FriendSnap
import kotlinx.coroutines.flow.Flow

/**
 * Inbox of snaps friends have sent to the current child today.
 * Real backend (Firestore subscription per child) will replace the stub.
 */
interface FriendSnapsRepository {
    fun snapsForCurrentChild(): Flow<List<FriendSnap>>
}
