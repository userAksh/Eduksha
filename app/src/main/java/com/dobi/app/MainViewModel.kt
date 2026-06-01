package com.dobi.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            // Use addAuthStateListener instead of reading currentUser directly.
            // After a device unlock Firebase decrypts its keystore async, so
            // currentUser can be null briefly even with a persisted user.
            val currentUser = suspendCancellableCoroutine { cont ->
                val listener = object : FirebaseAuth.AuthStateListener {
                    override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                        firebaseAuth.removeAuthStateListener(this)
                        if (cont.isActive) cont.resume(firebaseAuth.currentUser)
                    }
                }
                auth.addAuthStateListener(listener)
                cont.invokeOnCancellation { auth.removeAuthStateListener(listener) }
            }
            _startDestination.value = if (currentUser != null) {
                Screen.ParentHome
            } else {
                val isChildDevice = userPreferences.isChildDevice.first()
                val linkedChildId = userRepository.linkedChildId.first()
                if (isChildDevice && linkedChildId != null)
                    Screen.ChildHome(linkedChildId) else Screen.RoleSelection
            }
        }
    }
}
