package com.dobi.app.feature.parenthome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.model.ParentProfile
import com.dobi.app.domain.repository.AuthRepository
import com.dobi.app.domain.repository.ChildRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentHomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val childRepository: ChildRepository
) : ViewModel() {

    data class UiState(
        val profile: ParentProfile? = null,
        val children: List<Child> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _navToRoleSelection = MutableSharedFlow<Unit>()
    val navToRoleSelection = _navToRoleSelection.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val profile = authRepository.getParentProfile()
            _uiState.value = _uiState.value.copy(profile = profile)
            childRepository.getChildren(uid).collect { children ->
                _uiState.value = _uiState.value.copy(children = children, isLoading = false)
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        viewModelScope.launch { _navToRoleSelection.emit(Unit) }
    }
}
