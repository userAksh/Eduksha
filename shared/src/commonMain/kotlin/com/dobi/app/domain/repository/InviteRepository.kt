package com.dobi.app.domain.repository

import com.dobi.app.domain.model.ReferralReward
import kotlinx.coroutines.flow.Flow

interface InviteRepository {

    // ── Child invite (existing) ─────────────────────────────────────────────

    suspend fun createInviteShareUrl(childId: String): Result<String>
    suspend fun claimInvite(code: String, childId: String): Result<ClaimOutcome>
    suspend fun readPendingInviteCode(): String?

    // ── Parent referral (new) ───────────────────────────────────────────────

    /** Creates a parent-type invite. Returns Pair(shareUrl, code). */
    suspend fun createParentInviteShareUrl(): Result<Pair<String, String>>

    /** Claims a parent-type invite (no childId needed). Idempotent. */
    suspend fun claimParentInvite(code: String): Result<ClaimOutcome>

    /**
     * Called after the referee adds their first child. Server checks grade+board
     * match and writes friend edges + Stage 1 reward if matched.
     */
    suspend fun applyReferralMatch(childId: String, grade: Int, board: String): Result<Unit>

    /** Called after 7 days of activity. Server unlocks Stage 2 reward. */
    suspend fun recordReferralWeekActivity(): Result<Unit>

    /** Live stream of scratch-card rewards for the signed-in parent. */
    fun observeReferralRewards(): Flow<List<ReferralReward>>

    /** Marks a reward scratched and returns the revealed value. */
    suspend fun scratchReward(rewardId: String): Result<String>
}

enum class ClaimOutcome {
    CLAIMED,
    ALREADY_CLAIMED,
    SELF_INVITE,
    INVITER_MISSING
}
