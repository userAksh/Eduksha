package com.dobi.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.local.UserPreferences
import com.dobi.app.domain.model.UserRole
import com.dobi.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
            // Sync check first: Firebase loads the cached user before the first
            // onAuthStateChanged post arrives. Using it directly avoids any race
            // with the async listener on slow/cold starts.
            val currentUser = auth.currentUser ?: withTimeoutOrNull(4_000) {
                suspendCancellableCoroutine { cont ->
                    val listener = object : FirebaseAuth.AuthStateListener {
                        override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                            val user = firebaseAuth.currentUser ?: return
                            firebaseAuth.removeAuthStateListener(this)
                            if (cont.isActive) cont.resume(user)
                        }
                    }
                    auth.addAuthStateListener(listener)
                    cont.invokeOnCancellation { auth.removeAuthStateListener(listener) }
                }
            } ?: auth.currentUser
            _startDestination.value = if (currentUser != null) {
                Screen.Paywall
            } else {
                val isChildDevice = userPreferences.isChildDevice.first()
                val linkedChildId = userRepository.linkedChildId.first()
                when {
                    isChildDevice && linkedChildId != null -> Screen.ChildHome(linkedChildId)
                    userPreferences.userRole.first() == UserRole.PARENT ->
                        Screen.PhoneAuth
                    else -> Screen.RoleSelection
                }
            }
        }
    }
}
