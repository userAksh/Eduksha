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
        viewModelScope.launch {
            _startDestination.value = when {
                auth.currentUser != null -> Screen.ParentHome
                else -> {
                    val linkedChildId = userRepository.linkedChildId.first()
                    if (linkedChildId != null) Screen.ChildHome(linkedChildId)
                    else Screen.RoleSelection
                }
            }
        }
    }
}
