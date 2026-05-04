package com.dobi.app.feature.parenthome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ParentProfile
import com.dobi.app.domain.model.ParentTask
import com.dobi.app.domain.repository.AuthRepository
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.FcmTokenRepository
import com.dobi.app.domain.repository.PairingRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentHomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val childRepository: ChildRepository,
    private val pairingRepository: PairingRepository,
    private val parentTaskRepository: ParentTaskRepository,
    private val fcmTokenRepository: FcmTokenRepository
) : ViewModel() {

    data class UiState(
        val profile: ParentProfile? = null,
        val children: List<Child> = emptyList(),
        val parentTasks: List<ParentTask> = emptyList(),
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
        viewModelScope.launch { fcmTokenRepository.syncCurrentToken() }
    }

    private fun observeParentTasks() {
        parentTaskRepository.observeForActiveParent()
            .onEach { tasks ->
                _uiState.value = _uiState.value.copy(parentTasks = tasks)
            }
            .launchIn(viewModelScope)
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val profile = authRepository.getParentProfile()
            _uiState.value = _uiState.value.copy(profile = profile)
            childRepository.getChildren(uid).collect { children ->
                _uiState.value = _uiState.value.copy(children = children, isLoading = false)
            }
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
}
