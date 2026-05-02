package com.dobi.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Tracks whether the currently linked child has already shared today's
 * streak snap. Drives the Snap & Send button on the quiz result screen
 * and the Today's Task → Friend Snaps swap on the Streak dashboard.
 *
 * "Today" = local calendar day; flips at local midnight.
 */
interface StreakShareRepository {
    fun hasSharedToday(): Flow<Boolean>
    suspend fun markSharedToday()
}
