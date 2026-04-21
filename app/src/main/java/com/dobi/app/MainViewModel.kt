package com.dobi.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.model.UserRole
import com.dobi.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userRepository: UserRepository
) : ViewModel() {

    /** null = loading (DataStore not yet read); non-null = navigate */
    val startDestination = userRepository.userRole
        .map { role ->
            when (role) {
                UserRole.STUDENT -> Screen.ChildHome
                UserRole.PARENT -> Screen.ParentHome
                null -> Screen.RoleSelection
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )
}
