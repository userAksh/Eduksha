package com.dobi.app.feature.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.ClanInfo
import com.dobi.app.domain.model.ClanTier
import com.dobi.app.domain.model.StreakState
import com.dobi.app.domain.repository.FriendSnapsRepository
import com.dobi.app.domain.repository.StreakShareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backing state for the Streak & Clan Home screen.
 *
 * Streak / clan / friend-activity backend isn't built yet, so the static
 * snapshot below stands in. Live signals already wired:
 *  - [hasSharedToday] from [StreakShareRepository] — flips after the child
 *    fires the snap & send button on any quiz result.
 *  - [friendSnaps] from [FriendSnapsRepository] — currently empty stub;
 *    swap for the Firestore-backed feed when ready.
 */
@HiltViewModel
class StreakHomeViewModel @Inject constructor(
    streakShareRepository: StreakShareRepository,
    friendSnapsRepository: FriendSnapsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(seed())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                streakShareRepository.hasSharedToday(),
                friendSnapsRepository.snapsForCurrentChild()
            ) { hasShared, snaps -> hasShared to snaps }
                .collect { (hasShared, snaps) ->
                    _uiState.update {
                        it.copy(hasSharedToday = hasShared, friendSnaps = snaps)
                    }
                }
        }
    }

    private fun seed(): StreakHomeUiState = StreakHomeUiState(
        childName = "Emma",
        streak = StreakState(
            days = 0,
            lastUpdatedMillis = 0L,
            taskCompletedToday = false
        ),
        clan = ClanInfo(tier = ClanTier.NONE),
        friendsInvited = 0,
        friendsActivity = emptyList()
    )
}
