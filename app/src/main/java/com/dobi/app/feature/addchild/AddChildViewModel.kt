package com.dobi.app.feature.addchild

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.Child
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
class AddChildViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _navToParentHome = MutableSharedFlow<Unit>()
    val navToParentHome = _navToParentHome.asSharedFlow()

    private val _addedCount = MutableStateFlow(0)
    val addedCount = _addedCount.asStateFlow()

    fun addChild(name: String, grade: Int, board: String) {
        val parentId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val child = Child(name = name, grade = grade, board = board, parentId = parentId)
            childRepository.addChild(child)
                .onSuccess {
                    _addedCount.value += 1
                    _uiState.value = UiState.Success
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Failed to add child")
                }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    fun goToParentHome() {
        viewModelScope.launch { _navToParentHome.emit(Unit) }
    }
}
