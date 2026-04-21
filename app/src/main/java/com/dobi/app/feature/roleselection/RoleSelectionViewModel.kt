package com.dobi.app.feature.roleselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.repository.ChildRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoleSelectionViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository
) : ViewModel() {

    sealed class NavEvent {
        object ToPhoneAuth : NavEvent()
        object ToParentHome : NavEvent()
        object ToSelectChild : NavEvent()
        object ToPairingCode : NavEvent()
        object ShowNoChildrenDialog : NavEvent()
    }

    private val _navEvent = MutableSharedFlow<NavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    fun onParentSelected() {
        viewModelScope.launch {
            // Always require phone + OTP verification from Role Selection so a child
            // can't tap "I am a Parent" and reach the dashboard without authenticating.
            _navEvent.emit(NavEvent.ToPhoneAuth)
        }
    }

    fun onChildSelected() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                // No parent logged in on this device — child needs to pair via code
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
}
