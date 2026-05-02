package com.dobi.app.feature.streak

import com.dobi.app.domain.model.LeaderboardEntry
import com.dobi.app.domain.model.StreakReward

enum class LeaderboardWindow { THIS_WEEK, ALL_TIME }

data class ClanLeaderboardUiState(
    val window: LeaderboardWindow = LeaderboardWindow.THIS_WEEK,
    val entries: List<LeaderboardEntry> = emptyList(),
    val rewards: List<StreakReward> = emptyList(),
    val isLoading: Boolean = false
) {
    val topThree: List<LeaderboardEntry> get() = entries.take(3)
    val rest: List<LeaderboardEntry> get() = entries.drop(3)
}
