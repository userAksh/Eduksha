package com.dobi.app.feature.selectchild

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.repository.ChildRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectChildViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Ready(val children: List<Child>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadChildren()
    }

    private fun loadChildren() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            childRepository.getChildren(uid).collect { children ->
                _uiState.value = UiState.Ready(children)
            }
        }
    }
}
