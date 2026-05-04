package com.dobi.app.feature.childhome

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.ClaimOutcome
import com.dobi.app.domain.repository.FcmTokenRepository
import com.dobi.app.domain.repository.InviteRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import com.dobi.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChildHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val childRepository: ChildRepository,
    private val parentTaskRepository: ParentTaskRepository,
    private val fcmTokenRepository: FcmTokenRepository,
    private val inviteRepository: InviteRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val childId: String = savedStateHandle.toRoute<Screen.ChildHome>().childId

    private val _uiState = MutableStateFlow(ChildHomeUiState())
    val uiState = _uiState.asStateFlow()

    sealed interface InviteUiEvent {
        data class Share(val url: String) : InviteUiEvent
        data object CreateFailed : InviteUiEvent
    }

    private val _inviteEvents = MutableSharedFlow<InviteUiEvent>(extraBufferCapacity = 1)
    val inviteEvents = _inviteEvents.asSharedFlow()

    init {
        registerActiveChild()
        loadChild()
        observeTutor()
        observeParentTasks()
    }

    private fun observeParentTasks() {
        parentTaskRepository.observeForActiveChild()
            .onEach { tasks ->
                _uiState.value = _uiState.value.copy(parentTasks = tasks)
            }
            .launchIn(viewModelScope)
    }

    private fun observeTutor() {
        combine(userRepository.tutorHonorific, userRepository.tutorName) { h, n -> h to n }
            .onEach { (honorific, name) ->
                _uiState.value = _uiState.value.copy(
                    tutorHonorific = honorific,
                    tutorName = name
                )
            }
            .launchIn(viewModelScope)
    }

    fun saveTutor(honorific: String, name: String) {
        viewModelScope.launch {
            userRepository.saveTutor(honorific.trim(), name.trim())
        }
    }

    private fun registerActiveChild() {
        viewModelScope.launch {
            val parentId = auth.currentUser?.uid
                ?: userRepository.linkedParentId.first()
                ?: ""
            userRepository.saveLinkedChild(parentId, childId)
            // Sync after the linked-parent/child prefs are written so the
            // FCM token write knows where to land for child devices.
            fcmTokenRepository.syncCurrentToken()
            // Try to attribute a deferred install referrer to this child.
            // First child to open the app on this install gets the friend.
            tryClaimPendingInvite()
        }
    }

    private suspend fun tryClaimPendingInvite() {
        if (userPreferences.inviteClaimDone.first()) return
        val code = inviteRepository.readPendingInviteCode() ?: run {
            // No referrer at all → mark done so we stop polling on every
            // future cold start. New users without an invite stay clean.
            userPreferences.markInviteClaimDone()
            return
        }
        if (childId.isBlank()) return
        inviteRepository.claimInvite(code, childId)
            .onSuccess { outcome ->
                Log.i(TAG, "claimInvite outcome=$outcome")
                if (outcome != ClaimOutcome.SELF_INVITE) {
                    userPreferences.markInviteClaimDone()
                }
            }
            .onFailure {
                // Leave flag unset; we retry on the next ChildHome entry.
                Log.w(TAG, "claimInvite retryable failure", it)
            }
    }

    fun onInviteFriendClicked() {
        viewModelScope.launch {
            inviteRepository.createInviteShareUrl(childId)
                .onSuccess { url -> _inviteEvents.tryEmit(InviteUiEvent.Share(url)) }
                .onFailure { _inviteEvents.tryEmit(InviteUiEvent.CreateFailed) }
        }
    }

    private fun loadChild() {
        viewModelScope.launch {
            val parentId = auth.currentUser?.uid
                ?: userRepository.linkedParentId.first()
                ?: return@launch
            childRepository.getChild(parentId, childId)
                .onSuccess { child ->
                    _uiState.value = _uiState.value.copy(
                        childName = child.name,
                        friends = child.friends
                    )
                    userRepository.saveLinkedChildGrade(child.grade)
                }
        }
    }

    companion object {
        private const val TAG = "ChildHomeViewModel"
    }
}
