package com.dobi.app.feature.streak

import com.dobi.app.domain.model.ClanInfo
import com.dobi.app.domain.model.FriendActivityItem
import com.dobi.app.domain.model.FriendSnap
import com.dobi.app.domain.model.StreakState

data class StreakHomeUiState(
    val childName: String = "",
    val streak: StreakState = StreakState(),
    val clan: ClanInfo = ClanInfo(),
    val friendsInvited: Int = 0,
    val friendsNeededForBonus: Int = 3,
    val bonusPoints: Int = 50,
    val friendsActivity: List<FriendActivityItem> = emptyList(),
    val hasSharedToday: Boolean = false,
    val friendSnaps: List<FriendSnap> = emptyList(),
    val isLoading: Boolean = false
) {
    val tasksCompletedToday: Boolean get() = streak.taskCompletedToday
    val inviteSubtitle: String get() {
        val left = (friendsNeededForBonus - friendsInvited).coerceAtLeast(0)
        return if (left == 0) "Goal reached! Tap to invite more."
        else "Invite $left ${if (left == 1) "friend" else "friends"} and earn exclusive badge + $bonusPoints points"
    }
}
