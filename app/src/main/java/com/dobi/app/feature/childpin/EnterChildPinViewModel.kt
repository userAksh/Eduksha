package com.dobi.app.feature.childpin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.UserRepository
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
class EnterChildPinViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val childRepository: ChildRepository
) : ViewModel() {

    data class UiState(
        val childName: String = "",
        val isVerifying: Boolean = false,
        val error: String? = null
    )

    val childId: String = savedStateHandle.toRoute<Screen.EnterChildPin>().childId

    private var child: Child? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _navToChildHome = MutableSharedFlow<String>()
    val navToChildHome = _navToChildHome.asSharedFlow()

    init {
        loadChild()
    }

    private fun loadChild() {
        viewModelScope.launch {
            val parentId = auth.currentUser?.uid
                ?: userRepository.linkedParentId.first()
                ?: return@launch
            childRepository.getChild(parentId, childId)
                .onSuccess {
                    child = it
                    _uiState.value = _uiState.value.copy(childName = it.name)
                }
        }
    }

    fun verify(pin: String) {
        if (pin.length != 4) return
        val stored = child?.pin.orEmpty()
        if (stored.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "This profile has no PIN set. Please edit it from the parent view.")
            return
        }
        if (pin == stored) {
            viewModelScope.launch { _navToChildHome.emit(childId) }
        } else {
            _uiState.value = _uiState.value.copy(error = "Incorrect PIN. Try again.")
        }
    }

    fun clearError() {
        if (_uiState.value.error != null) _uiState.value = _uiState.value.copy(error = null)
    }
}
