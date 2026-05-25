package com.dobi.app.domain.repository

interface InviteRepository {

    /**
     * Asks the server for a fresh invite code tied to [childId]. Returns the
     * Play Store URL that the inviter shares — friend taps it, installs, and
     * Play Store passes `referrer=invite_{code}` to the new install.
     */
    suspend fun createInviteShareUrl(childId: String): Result<String>

    /**
     * Idempotent on the server. Safe to call repeatedly while we still have
     * an unclaimed install referrer in the prefs.
     */
    suspend fun claimInvite(code: String, childId: String): Result<ClaimOutcome>

    /**
     * Reads the Play Install Referrer once, returns the embedded invite code
     * if present (i.e. referrer matched `invite_{code}`). Returns null on no
     * referrer, malformed referrer, or any client error — caller treats null
     * as "nothing to claim, try again next cold start".
     */
    suspend fun readPendingInviteCode(): String?
}

enum class ClaimOutcome {
    /** Friend edges were written and the inviter was notified. */
    CLAIMED,
    /** Server saw this child had already claimed this code; no-op. */
    ALREADY_CLAIMED,
    /** Caller is the inviter; can't befriend yourself. */
    SELF_INVITE,
    /** Inviter child was deleted before we could claim. */
    INVITER_MISSING
}
