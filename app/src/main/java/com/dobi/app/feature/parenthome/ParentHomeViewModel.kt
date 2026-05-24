package com.dobi.app.feature.parenthome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.repository.ChildEngagementRepository
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ChildEngagement
import com.dobi.app.domain.model.ParentProfile
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.repository.AuthRepository
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.FcmTokenRepository
import com.dobi.app.domain.repository.PairingRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
    private val engagementRepository: ChildEngagementRepository
) : ViewModel() {

    data class UiState(
        val profile: ParentProfile? = null,
        val children: List<Child> = emptyList(),
        val parentTasks: List<ParentTask> = emptyList(),
        val engagements: Map<String, ChildEngagement> = emptyMap(),
        val isLoading: Boolean = true
    )

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

    init {
        loadData()
        observeParentTasks()
        observeEngagements()
        viewModelScope.launch { fcmTokenRepository.syncCurrentToken() }
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
        val uid = auth.currentUser?.uid ?: return
        // Children and profile load in parallel. isLoading clears as soon as
        // children arrive — the header name fills in a moment later once the
        // profile fetch completes.
        viewModelScope.launch {
            childRepository.getChildren(uid).collect { children ->
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
}
