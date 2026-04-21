package com.dobi.app.feature.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.OtpState
import com.dobi.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    data class PrefillState(
        val name: String = "",
        val phone: String = "",
        val isReturning: Boolean = false
    )

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
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

    private val _prefill = MutableStateFlow(PrefillState())
    val prefill = _prefill.asStateFlow()

    private val _navEvent = MutableSharedFlow<NavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    init {
        loadPrefill()
    }

    private fun loadPrefill() {
        viewModelScope.launch {
            val existing = authRepository.getParentProfile()
            if (existing != null) {
                // Strip +91 prefix for the input field
                val localPhone = existing.phone.removePrefix("+91").removePrefix("91").takeLast(10)
                _prefill.value = PrefillState(
                    name = existing.name,
                    phone = localPhone,
                    isReturning = true
                )
            }
        }
    }

    fun sendOtp(name: String, phone: String, activity: Activity) {
        viewModelScope.launch {
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
        _navEvent.emit(NavEvent.ToParentHome)
        _uiState.value = UiState.Idle
    }
}
