package com.dobi.app.feature.auth

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.analytics.AnalyticsTracker
import com.dobi.app.analytics.Events
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.model.OtpState
import com.dobi.app.domain.model.UserRole
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
    private val authRepository: AuthRepository,
    private val userPreferences: com.dobi.app.data.local.UserPreferences,
    private val analytics: AnalyticsTracker,
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
    private var verificationId: String = route.verificationId
    private val name: String = route.name

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<NavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                authRepository.verifyOtp(verificationId, otp)
                    .onSuccess {
                        val existing = authRepository.getParentProfile()
                        if (existing == null) {
                            authRepository.saveParentProfile(name, "+91$phone")
                        }
                        userPreferences.saveRole(UserRole.PARENT)
                        analytics.log(Events.OTP_VERIFIED)
                        _navEvent.emit(NavEvent.ToParentHome)
                        _uiState.value = UiState.Idle
                    }
                    .onFailure { e ->
                        _uiState.value = UiState.Error(e.message ?: "Invalid OTP. Please try again.")
                    }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Something went wrong. Please try again.")
            }
        }
    }

    fun saveReferralCode(code: String) {
        val trimmed = code.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            userPreferences.saveParentReferralCode(trimmed)
            analytics.log(Events.REFERRAL_CODE_APPLIED) { putString("code", trimmed) }
        }
    }

    fun resendOtp(activity: Activity) {
        viewModelScope.launch {
            authRepository.sendOtp("+91$phone", activity).collect { state ->
                when (state) {
                    is OtpState.Sending -> _uiState.value = UiState.Loading
                    is OtpState.CodeSent -> {
                        verificationId = state.verificationId
                        _uiState.value = UiState.Idle
                    }
                    is OtpState.AutoVerified -> {
                        _uiState.value = UiState.Loading
                        val existing = authRepository.getParentProfile()
                        if (existing == null) {
                            authRepository.saveParentProfile(name, "+91$phone")
                        }
                        userPreferences.saveRole(UserRole.PARENT)
                        analytics.log(Events.OTP_VERIFIED)
                        _navEvent.emit(NavEvent.ToParentHome)
                        _uiState.value = UiState.Idle
                    }
                    is OtpState.Error -> _uiState.value = UiState.Error(state.message)
                }
            }
        }
    }
}
