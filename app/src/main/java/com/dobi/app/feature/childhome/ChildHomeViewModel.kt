package com.dobi.app.feature.childhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChildHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val childRepository: ChildRepository
) : ViewModel() {

    private val childId: String = savedStateHandle.toRoute<Screen.ChildHome>().childId

    private val _uiState = MutableStateFlow(ChildHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        registerActiveChild()
        loadChild()
    }

    private fun registerActiveChild() {
        viewModelScope.launch {
            val parentId = auth.currentUser?.uid
                ?: userRepository.linkedParentId.first()
                ?: ""
            userRepository.saveLinkedChild(parentId, childId)
        }
    }

    private fun loadChild() {
        viewModelScope.launch {
            val parentId = auth.currentUser?.uid
                ?: userRepository.linkedParentId.first()
                ?: return@launch
            childRepository.getChild(parentId, childId)
                .onSuccess { child ->
                    _uiState.value = _uiState.value.copy(childName = child.name)
                    userRepository.saveLinkedChildGrade(child.grade)
                }
        }
    }
}
