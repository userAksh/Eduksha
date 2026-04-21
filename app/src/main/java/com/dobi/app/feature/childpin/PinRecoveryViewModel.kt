package com.dobi.app.feature.childpin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.Child
import com.dobi.app.domain.repository.ChildRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinRecoveryViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val signedIn: Boolean = false,
        val children: List<Child> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadChildren()
    }

    private fun loadChildren() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = UiState(isLoading = false, signedIn = false)
            return
        }
        viewModelScope.launch {
            val children = childRepository.getChildren(uid).first()
            _uiState.value = UiState(isLoading = false, signedIn = true, children = children)
        }
    }
}
