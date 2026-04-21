package com.dobi.app.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class NavEvent {
        object ToParentHome : NavEvent()
    }

    private val route = savedStateHandle.toRoute<Screen.OtpVerification>()
    val phone: String = route.phone
    private val verificationId: String = route.verificationId
    private val name: String = route.name

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<NavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            authRepository.verifyOtp(verificationId, otp)
                .onSuccess {
                    val existing = authRepository.getParentProfile()
                    if (existing == null) {
                        authRepository.saveParentProfile(name, "+91$phone")
                    }
                    _navEvent.emit(NavEvent.ToParentHome)
                    _uiState.value = UiState.Idle
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Invalid OTP. Please try again.")
                }
        }
    }
}
