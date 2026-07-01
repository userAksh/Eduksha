package com.dobi.app.feature.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.analytics.AnalyticsTracker
import com.dobi.app.analytics.Events
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.model.OtpState
import com.dobi.app.domain.model.UserRole
import com.dobi.app.domain.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PhoneAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val analytics: AnalyticsTracker,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
        // Login mode only: phone number has no account — auto-switches to Sign Up tab
        data class NotRegistered(val message: String) : UiState()
    }

    sealed class NavEvent {
        data class ToOtpVerification(
            val verificationId: String,
            val name: String,
            val phone: String
        ) : NavEvent()
        object ToParentHome : NavEvent()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<NavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    fun sendOtp(name: String, phone: String, activity: Activity) {
        val isLoginMode = name.isBlank()
        viewModelScope.launch {
            // For Login mode, verify the number is registered before wasting an OTP.
            if (isLoginMode) {
                _uiState.value = UiState.Loading
                val isRegistered = try {
                    // Phone numbers are stored as "+91XXXXXXXXXX" for new accounts.
                    val result = firestore.collection("parents")
                        .whereEqualTo("phone", phone)
                        .limit(1)
                        .get()
                        .await()
                    !result.isEmpty
                } catch (_: Exception) {
                    // If the check fails, allow OTP to proceed and let post-OTP check catch it.
                    true
                }
                if (!isRegistered) {
                    _uiState.value = UiState.NotRegistered(
                        "This number isn't registered yet. Please sign up."
                    )
                    return@launch
                }
                _uiState.value = UiState.Idle
            }

            analytics.log(Events.PHONE_AUTH_INITIATED)
            authRepository.sendOtp("+91$phone", activity).collect { state ->
                when (state) {
                    is OtpState.Sending -> _uiState.value = UiState.Loading
                    is OtpState.CodeSent -> {
                        _uiState.value = UiState.Idle
                        _navEvent.emit(NavEvent.ToOtpVerification(state.verificationId, name, phone))
                    }
                    is OtpState.AutoVerified -> {
                        _uiState.value = UiState.Loading
                        handleAuthSuccess(name, phone)
                    }
                    is OtpState.Error -> _uiState.value = UiState.Error(state.message)
                }
            }
        }
    }

    private suspend fun handleAuthSuccess(name: String, phone: String) {
        val existing = authRepository.getParentProfile()
        if (existing == null) {
            authRepository.saveParentProfile(name, phone)
        }
        userPreferences.saveRole(UserRole.PARENT)
        _navEvent.emit(NavEvent.ToParentHome)
        _uiState.value = UiState.Idle
    }
}
