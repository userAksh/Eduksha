package com.dobi.app.feature.roleselection

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.OtpState
import com.dobi.app.domain.repository.AuthRepository
import com.dobi.app.domain.repository.ChildRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoleSelectionViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val childRepository: ChildRepository
) : ViewModel() {

    sealed class NavEvent {
        object ToPhoneAuth : NavEvent()
        object ToParentHome : NavEvent()
        object ToSelectChild : NavEvent()
        object ToPairingCode : NavEvent()
        object ShowNoChildrenDialog : NavEvent()
    }

    data class ReauthState(
        val visible: Boolean = false,
        val name: String = "",
        val phone: String = "",               // "+91..." full format
        val displayPhone: String = "",        // "+91 98765 43210" for UI
        val verificationId: String? = null,   // non-null = OTP has been sent
        val isSending: Boolean = false,
        val isVerifying: Boolean = false,
        val error: String? = null
    )

    private val _navEvent = MutableSharedFlow<NavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    private val _reauth = MutableStateFlow(ReauthState())
    val reauth = _reauth.asStateFlow()

    fun onParentSelected() {
        viewModelScope.launch {
            val signedIn = auth.currentUser != null
            if (!signedIn) {
                _navEvent.emit(NavEvent.ToPhoneAuth)
                return@launch
            }
            val profile = authRepository.getParentProfile()
            if (profile == null || profile.phone.isBlank()) {
                _navEvent.emit(NavEvent.ToPhoneAuth)
                return@launch
            }
            _reauth.value = ReauthState(
                visible = true,
                name = profile.name,
                phone = profile.phone,
                displayPhone = formatPhone(profile.phone)
            )
        }
    }

    fun dismissReauth() {
        _reauth.value = ReauthState()
    }

    fun sendOtp(activity: Activity) {
        val state = _reauth.value
        if (!state.visible || state.isSending) return
        _reauth.value = state.copy(isSending = true, error = null)
        viewModelScope.launch {
            authRepository.sendOtp(state.phone, activity).collect { otpState ->
                when (otpState) {
                    is OtpState.Sending -> {}
                    is OtpState.CodeSent -> {
                        _reauth.value = _reauth.value.copy(
                            isSending = false,
                            verificationId = otpState.verificationId
                        )
                    }
                    is OtpState.AutoVerified -> {
                        _reauth.value = _reauth.value.copy(isSending = false, isVerifying = false)
                        _navEvent.emit(NavEvent.ToParentHome)
                        dismissReauth()
                    }
                    is OtpState.Error -> {
                        _reauth.value = _reauth.value.copy(
                            isSending = false,
                            error = otpState.message
                        )
                    }
                }
            }
        }
    }

    fun verifyOtp(otp: String) {
        val state = _reauth.value
        val vid = state.verificationId ?: return
        if (otp.length != 6 || state.isVerifying) return
        _reauth.value = state.copy(isVerifying = true, error = null)
        viewModelScope.launch {
            authRepository.verifyOtp(vid, otp)
                .onSuccess {
                    _navEvent.emit(NavEvent.ToParentHome)
                    dismissReauth()
                }
                .onFailure { e ->
                    _reauth.value = _reauth.value.copy(
                        isVerifying = false,
                        error = e.message ?: "Invalid OTP. Please try again."
                    )
                }
        }
    }

    fun onChildSelected() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                _navEvent.emit(NavEvent.ToPairingCode)
                return@launch
            }
            val children = childRepository.getChildren(uid).first()
            if (children.isEmpty()) {
                _navEvent.emit(NavEvent.ShowNoChildrenDialog)
            } else {
                _navEvent.emit(NavEvent.ToSelectChild)
            }
        }
    }

    private fun formatPhone(raw: String): String {
        // Assume +91 prefix; format as "+91 98765 43210"
        val digits = raw.removePrefix("+91").removePrefix("91").filter { it.isDigit() }.takeLast(10)
        return if (digits.length == 10) "+91 ${digits.substring(0, 5)} ${digits.substring(5)}" else raw
    }
}
