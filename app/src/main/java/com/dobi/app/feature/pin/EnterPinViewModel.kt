package com.dobi.app.feature.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnterPinViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Verifying : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _navToParentHome = MutableSharedFlow<Unit>()
    val navToParentHome = _navToParentHome.asSharedFlow()

    fun verify(pin: String) {
        if (pin.length != 4) return
        viewModelScope.launch {
            _uiState.value = UiState.Verifying
            val ok = authRepository.verifyPin(pin)
            if (ok) {
                _navToParentHome.emit(Unit)
                _uiState.value = UiState.Idle
            } else {
                _uiState.value = UiState.Error("Incorrect PIN. Please try again.")
            }
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) _uiState.value = UiState.Idle
    }
}
