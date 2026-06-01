package com.dobi.app.feature.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.repository.PairingRepository
import com.dobi.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnterPairingCodeViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Verifying : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _navToChildHome = MutableSharedFlow<String>()
    val navToChildHome = _navToChildHome.asSharedFlow()

    fun verifyCode(code: String) {
        if (code.length != 6) return
        viewModelScope.launch {
            _uiState.value = UiState.Verifying
            pairingRepository.redeemCode(code)
                .onSuccess { pairing ->
                    userRepository.saveLinkedChild(pairing.parentId, pairing.childId)
                    userPreferences.markAsChildDevice()
                    _navToChildHome.emit(pairing.childId)
                    _uiState.value = UiState.Idle
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Couldn't verify code. Please try again.")
                }
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) _uiState.value = UiState.Idle
    }
}
