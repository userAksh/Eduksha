package com.dobi.app.feature.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.ChildEngagementRepository
import com.dobi.app.domain.model.ClanInfo
import com.dobi.app.domain.model.ClanTier
import com.dobi.app.domain.model.StreakState
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.FriendSnapsRepository
import com.dobi.app.domain.repository.StreakShareRepository
import com.dobi.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StreakHomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val engagementRepository: ChildEngagementRepository,
    private val childRepository: ChildRepository,
    private val streakShareRepository: StreakShareRepository,
    private val friendSnapsRepository: FriendSnapsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreakHomeUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userRepository.linkedChildId,
                userRepository.linkedParentId
            ) { childId, parentId -> childId to parentId }
                .flatMapLatest { (childId, parentId) ->
                    if (childId.isNullOrBlank() || parentId.isNullOrBlank()) {
                        flowOf(StreakHomeUiState(isLoading = false))
                    } else {
                        liveState(childId, parentId)
                    }
                }
                .collect { _uiState.value = it }
        }
    }

    private fun liveState(childId: String, parentId: String) = combine(
        flow { emit(childRepository.getChild(parentId, childId).getOrNull()) },
        engagementRepository.observeEngagement(childId),
        streakShareRepository.hasSharedToday(),
        friendSnapsRepository.snapsForCurrentChild()
    ) { child, eng, hasShared, snaps ->
        val friendsCount = child?.friends?.size ?: 0
        val tier = ClanTier.resolve(friendsCount, eng.streakDays)
        StreakHomeUiState(
            childName = child?.name?.split(" ")?.firstOrNull() ?: "",
            streak = StreakState(
                days = eng.streakDays,
                lastUpdatedMillis = eng.lastActivityMs,
                taskCompletedToday = eng.isActiveToday
            ),
            clan = ClanInfo(tier = tier),
            friendsInvited = friendsCount,
            friendsNeededForBonus = ClanTier.STREAK_WARRIOR.friendThreshold,
            hasSharedToday = hasShared,
            friendSnaps = snaps,
            isLoading = false
        )
    }
}
