package com.dobi.app

import androidx.lifecycle.ViewModel
import com.dobi.app.core.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    auth: FirebaseAuth
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Screen?>(
        if (auth.currentUser != null) Screen.ParentHome else Screen.RoleSelection
    )
    val startDestination = _startDestination.asStateFlow()
}
