package com.dobi.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    auth: FirebaseAuth,
    userRepository: UserRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        // auth.currentUser is an in-memory cached value — no IO, no suspend needed.
        // Resolve synchronously so MainActivity never shows the blank DobiBackground frame
        // on the common logged-in-parent path.
        if (auth.currentUser != null) {
            _startDestination.value = Screen.ParentHome
        } else {
            viewModelScope.launch {
                val linkedChildId = userRepository.linkedChildId.first()
                _startDestination.value = if (linkedChildId != null)
                    Screen.ChildHome(linkedChildId) else Screen.RoleSelection
            }
        }
    }
}
