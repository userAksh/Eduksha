package com.dobi.app.feature.streak

import androidx.lifecycle.ViewModel
import com.dobi.app.domain.model.ClanTier
import com.dobi.app.domain.model.LeaderboardEntry
import com.dobi.app.domain.model.StreakReward
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Backing state for the Clan Leaderboard screen.
 *
 * Static seed for now — same caveat as [StreakHomeViewModel]: backend
 * (clan ranking + rewards) isn't built yet, so this VM emits a snapshot
 * matching the Figma mock.
 */
@HiltViewModel
class ClanLeaderboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(seed())
    val uiState = _uiState.asStateFlow()

    fun onWindowChange(window: LeaderboardWindow) {
        _uiState.update { it.copy(window = window) }
    }

    private fun seed(): ClanLeaderboardUiState = ClanLeaderboardUiState(
        entries = listOf(
            LeaderboardEntry(
                rank = 1,
                clanName = ClanTier.NEXT_GEN_CHAMPS.displayLabel,
                tier = ClanTier.NEXT_GEN_CHAMPS,
                points = 2480
            ),
            LeaderboardEntry(
                rank = 2,
                clanName = ClanTier.RISING_LEGENDS.displayLabel,
                tier = ClanTier.RISING_LEGENDS,
                points = 1320
            ),
            LeaderboardEntry(
                rank = 3,
                clanName = ClanTier.STREAK_WARRIOR.displayLabel,
                tier = ClanTier.STREAK_WARRIOR,
                points = 540,
                isCurrentClan = true
            )
        ),
        rewards = listOf(
            StreakReward(
                id = "badge",
                title = "Streak Badge",
                subtitle = "7 Day Streak",
                emoji = "🛡️",
                unlocked = true
            ),
            StreakReward(
                id = "xp",
                title = "XP Boost",
                subtitle = "2x XP 1 Day",
                emoji = "⚡",
                unlocked = true
            ),
            StreakReward(
                id = "mystery",
                title = "Mystery Box",
                subtitle = "5 Day Streak",
                emoji = "🎁",
                unlocked = false
            )
        )
    )
}
