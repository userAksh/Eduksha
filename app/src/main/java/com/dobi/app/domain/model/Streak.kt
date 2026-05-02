package com.dobi.app.domain.model

/**
 * Tier the child has unlocked in the Clan ladder. Lower ordinal = higher
 * achievement (Clan 1 is the top tier on the whiteboard spec).
 */
/**
 * Three fixed clan tiers a child auto-promotes through as they invite friends
 * and maintain their streak. Lowest is [STREAK_WARRIOR] (entry tier),
 * highest is [NEXT_GEN_CHAMPS]. [NONE] = haven't qualified for any clan yet.
 */
enum class ClanTier(
    val displayLabel: String,
    val friendThreshold: Int,
    val streakThreshold: Int,
    val pointsReward: Int
) {
    NEXT_GEN_CHAMPS("Next Gen Champs", 25, 30, 2000),
    RISING_LEGENDS("Rising Legends", 5, 10, 500),
    STREAK_WARRIOR("Streak Warrior", 2, 3, 0),
    NONE("", 0, 0, 0);

    companion object {
        fun resolve(friends: Int, streakDays: Int): ClanTier = when {
            friends >= NEXT_GEN_CHAMPS.friendThreshold &&
                streakDays >= NEXT_GEN_CHAMPS.streakThreshold -> NEXT_GEN_CHAMPS
            friends >= RISING_LEGENDS.friendThreshold &&
                streakDays >= RISING_LEGENDS.streakThreshold -> RISING_LEGENDS
            friends >= STREAK_WARRIOR.friendThreshold &&
                streakDays >= STREAK_WARRIOR.streakThreshold -> STREAK_WARRIOR
            else -> NONE
        }
    }
}

data class ClanInfo(
    val tier: ClanTier = ClanTier.NONE,
    val emoji: String = "🛡️",
    val rankInSchool: Int = 0,
    val pointsEarned: Int = 0,
    val pointsToNextTier: Int = 0
) {
    val name: String get() = tier.displayLabel
}

data class StreakState(
    val days: Int = 0,
    val lastUpdatedMillis: Long = 0L,
    val taskCompletedToday: Boolean = false
)

data class FriendActivityItem(
    val id: String = "",
    val name: String = "",
    val avatarEmoji: String = "🧑",
    val action: String = "",
    val streakDays: Int = 0,
    val timestampMillis: Long = 0L
)

data class LeaderboardEntry(
    val rank: Int = 0,
    val clanName: String = "",
    val emoji: String = "🛡️",
    val tier: ClanTier? = null,
    val points: Int = 0,
    val isCurrentClan: Boolean = false
)

data class StreakReward(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val emoji: String = "🎁",
    val unlocked: Boolean = false
)
