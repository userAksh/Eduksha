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

data class ChildForm(
    val id: Int,
    val name: String = "",
    val grade: Int = 5,
    val board: String = "CBSE"
)

@HiltViewModel
class AddChildViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Saving : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _forms = MutableStateFlow(listOf(ChildForm(id = 0)))
    val forms = _forms.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _navToParentHome = MutableSharedFlow<Unit>()
    val navToParentHome = _navToParentHome.asSharedFlow()

    fun addForm() {
        val nextId = (_forms.value.maxOfOrNull { it.id } ?: -1) + 1
        _forms.value = _forms.value + ChildForm(id = nextId)
    }

    fun removeForm(id: Int) {
        if (_forms.value.size > 1) {
            _forms.value = _forms.value.filter { it.id != id }
        }
    }

    fun updateName(id: Int, name: String) {
        _forms.value = _forms.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    fun updateGrade(id: Int, grade: Int) {
        _forms.value = _forms.value.map { if (it.id == id) it.copy(grade = grade) else it }
    }

    fun updateBoard(id: Int, board: String) {
        _forms.value = _forms.value.map { if (it.id == id) it.copy(board = board) else it }
    }

    fun saveAll() {
        val parentId = auth.currentUser?.uid ?: return
        val validForms = _forms.value.filter { it.name.isNotBlank() }
        if (validForms.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = UiState.Saving
            var hasError = false
            validForms.forEach { form ->
                childRepository.addChild(
                    Child(name = form.name.trim(), grade = form.grade, board = form.board, parentId = parentId)
                ).onFailure { hasError = true }
            }
            if (hasError) {
                _uiState.value = UiState.Error("Some children couldn't be saved. Please try again.")
            } else {
                _navToParentHome.emit(Unit)
            }
        }
    }
}
