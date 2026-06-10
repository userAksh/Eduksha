package com.dobi.app.feature.parenthome

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.analytics.AnalyticsTracker
import com.dobi.app.analytics.Events
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.data.repository.ChildEngagementRepository
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ChildEngagement
import com.dobi.app.domain.model.ParentProfile
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.model.ReferralReward
import com.dobi.app.domain.repository.AuthRepository
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.FcmTokenRepository
import com.dobi.app.domain.repository.InviteRepository
import com.dobi.app.domain.repository.PairingRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ParentHomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val childRepository: ChildRepository,
    private val pairingRepository: PairingRepository,
    private val parentTaskRepository: ParentTaskRepository,
    private val fcmTokenRepository: FcmTokenRepository,
    private val engagementRepository: ChildEngagementRepository,
    private val inviteRepository: InviteRepository,
    private val userPreferences: UserPreferences,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    data class UiState(
        val profile: ParentProfile? = null,
        val children: List<Child> = emptyList(),
        val parentTasks: List<ParentTask> = emptyList(),
        val engagements: Map<String, ChildEngagement> = emptyMap(),
        val isLoading: Boolean = true
    )

    sealed class InviteEvent {
        data class Share(val text: String) : InviteEvent()
    }

    sealed class PairingDialogState {
        object Hidden : PairingDialogState()
        data class Loading(val child: Child) : PairingDialogState()
        data class Shown(val child: Child, val code: String) : PairingDialogState()
        data class Error(val child: Child, val message: String) : PairingDialogState()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _pairingDialog = MutableStateFlow<PairingDialogState>(PairingDialogState.Hidden)
    val pairingDialog = _pairingDialog.asStateFlow()

    private val _navToRoleSelection = MutableSharedFlow<Unit>()
    val navToRoleSelection = _navToRoleSelection.asSharedFlow()

    private val _inviteEvent = MutableSharedFlow<InviteEvent>(extraBufferCapacity = 1)
    val inviteEvent = _inviteEvent.asSharedFlow()

    val referralRewards = inviteRepository.observeReferralRewards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadData()
        observeParentTasks()
        observeEngagements()
        viewModelScope.launch { fcmTokenRepository.syncCurrentToken() }
        viewModelScope.launch { tryClaimParentReferral() }
        viewModelScope.launch { trackFirstActiveAndCheckMilestone() }
        analytics.log(Events.PARENT_HOME_REACHED)
    }

    private fun observeParentTasks() {
        parentTaskRepository.observeForActiveParent()
            .onEach { tasks ->
                _uiState.value = _uiState.value.copy(parentTasks = tasks)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Watches the children list and re-subscribes engagement observers per
     * child whenever it changes. Each engagement flow listens to that child's
     * uploads, subjective_results, and quiz_attempts; we combine them all
     * into a single id → snapshot map so the screen can render in one read.
     */
    private fun observeEngagements() {
        _uiState
            .map { it.children.map(Child::id) }
            .distinctUntilChanged()
            .flatMapLatest { childIds ->
                if (childIds.isEmpty()) {
                    flowOf(emptyMap<String, ChildEngagement>())
                } else {
                    combine(childIds.map { engagementRepository.observeEngagement(it) }) { snapshots ->
                        snapshots.associateBy { it.childId }
                    }
                }
            }
            .onEach { engagements ->
                _uiState.value = _uiState.value.copy(engagements = engagements)
            }
            .launchIn(viewModelScope)
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid
        Log.d(TAG, "loadData: uid=$uid")
        if (uid == null) { Log.w(TAG, "loadData: auth.currentUser is null — returning early, isLoading stays true"); return }
        viewModelScope.launch {
            Log.d(TAG, "loadData: starting getChildren listener for uid=$uid")
            childRepository.getChildren(uid).collect { children ->
                Log.d(TAG, "loadData: getChildren emitted count=${children.size} ids=${children.map { it.id }}")
                _uiState.value = _uiState.value.copy(children = children, isLoading = false)
            }
        }
        viewModelScope.launch {
            val profile = authRepository.getParentProfile()
            _uiState.value = _uiState.value.copy(profile = profile)
        }
    }

    fun onPairDeviceTapped(child: Child) {
        val parentId = auth.currentUser?.uid ?: return
        _pairingDialog.value = PairingDialogState.Loading(child)
        viewModelScope.launch {
            pairingRepository.generateCode(parentId, child.id)
                .onSuccess { pairing ->
                    analytics.log(Events.PAIRING_CODE_GENERATED)
                    _pairingDialog.value = PairingDialogState.Shown(child, pairing.code)
                }
                .onFailure {
                    _pairingDialog.value = PairingDialogState.Error(
                        child,
                        it.message ?: "Couldn't generate code. Try again."
                    )
                }
        }
    }

    fun dismissPairingDialog() {
        _pairingDialog.value = PairingDialogState.Hidden
    }

    fun signOut() {
        authRepository.signOut()
        viewModelScope.launch { _navToRoleSelection.emit(Unit) }
    }

    fun sendReminder(taskId: String) {
        if (taskId.isBlank()) return
        viewModelScope.launch { parentTaskRepository.sendReminder(taskId) }
    }

    fun onInviteParentsClicked() {
        viewModelScope.launch {
            inviteRepository.createParentInviteShareUrl()
                .onSuccess { (url, code) ->
                    val name = _uiState.value.profile?.name?.substringBefore(" ")?.ifBlank { null }
                    val greeting = if (name != null) "Hi! $name invited you to Eduksha" else "Hi! You've been invited to Eduksha"
                    val text = "$greeting — the app that helps parents track their child's learning.\n\n" +
                        "📚 Set tasks, review homework, and stay connected with your child's progress.\n\n" +
                        "Download here 👇\n$url\n\n" +
                        "Or use referral code: ${code.uppercase()} when signing up."
                    analytics.log(Events.INVITE_SHARED) { putString("type", "parent") }
                    _inviteEvent.tryEmit(InviteEvent.Share(text))
                }
                .onFailure { Log.w(TAG, "createParentInvite failed", it) }
        }
    }

    fun scratchReward(rewardId: String, onRevealed: (String) -> Unit) {
        viewModelScope.launch {
            inviteRepository.scratchReward(rewardId)
                .onSuccess { value ->
                    analytics.log(Events.REWARD_SCRATCHED)
                    onRevealed(value)
                }
                .onFailure { Log.w(TAG, "scratchReward failed", it) }
        }
    }

    private suspend fun tryClaimParentReferral() {
        if (userPreferences.parentReferralClaimDone.first()) return
        // Try manual code first (entered on OTP screen), then install referrer.
        val code = userPreferences.parentReferralCode.first()
            ?: inviteRepository.readPendingInviteCode()
            ?: run { userPreferences.markParentReferralClaimDone(); return }
        inviteRepository.claimParentInvite(code)
            .onSuccess {
                Log.i(TAG, "parent referral claimed: code=$code")
                analytics.log(Events.REFERRAL_CLAIMED) { putString("type", "parent") }
            }
            .onFailure { Log.w(TAG, "parent referral claim failed", it) }
        userPreferences.markParentReferralClaimDone()
    }

    private suspend fun trackFirstActiveAndCheckMilestone() {
        userPreferences.saveParentFirstActiveAt(System.currentTimeMillis())
        val firstActive = userPreferences.parentFirstActiveAt.first()
        if (firstActive <= 0L) return
        val weekMs = 7L * 24 * 60 * 60 * 1000
        if (System.currentTimeMillis() - firstActive >= weekMs) {
            inviteRepository.recordReferralWeekActivity()
                .onSuccess { Log.i(TAG, "week milestone recorded") }
                .onFailure { Log.w(TAG, "week milestone failed", it) }
        }
    }

    companion object {
        private const val TAG = "ParentHomeVM"
    }
}
