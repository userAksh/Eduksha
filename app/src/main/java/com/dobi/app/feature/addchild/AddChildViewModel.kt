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

enum class Gender(val display: String, val emoji: String, val key: String) {
    BOY("Boy", "👦", "boy"),
    GIRL("Girl", "👧", "girl"),
    OTHER("Other", "🧒", "other");

    companion object {
        fun fromKey(key: String): Gender = entries.find { it.key == key } ?: BOY
    }
}

data class ChildDraft(
    val name: String = "",
    val gender: Gender = Gender.BOY,
    val grade: Int = 5,
    val board: String = "CBSE"
)

enum class AddChildStep { WELCOME, DETAILS, DONE }

@HiltViewModel
class AddChildViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository
) : ViewModel() {

    private val _step = MutableStateFlow(AddChildStep.WELCOME)
    val step = _step.asStateFlow()

    private val _draft = MutableStateFlow(ChildDraft())
    val draft = _draft.asStateFlow()

    private val _addedChildren = MutableStateFlow<List<Child>>(emptyList())
    val addedChildren = _addedChildren.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _editingChildId = MutableStateFlow<String?>(null)
    val editingChildId = _editingChildId.asStateFlow()

    private val _navToParentHome = MutableSharedFlow<Unit>()
    val navToParentHome = _navToParentHome.asSharedFlow()

    fun startAdding() {
        _editingChildId.value = null
        _draft.value = ChildDraft()
        _step.value = AddChildStep.DETAILS
    }

    fun goBack() {
        _step.value = when (_step.value) {
            AddChildStep.DETAILS -> {
                _editingChildId.value = null
                _draft.value = ChildDraft()
                AddChildStep.WELCOME
            }
            AddChildStep.DONE -> {
                // Prefill with the most recently added child so the user can edit it
                _addedChildren.value.lastOrNull()?.let { child ->
                    _editingChildId.value = child.id
                    _draft.value = ChildDraft(
                        name = child.name,
                        gender = Gender.fromKey(child.gender),
                        grade = child.grade,
                        board = child.board
                    )
                }
                AddChildStep.DETAILS
            }
            else -> _step.value
        }
    }

    fun updateName(v: String) { _draft.value = _draft.value.copy(name = v) }
    fun updateGender(v: Gender) { _draft.value = _draft.value.copy(gender = v) }
    fun updateGrade(v: Int) { _draft.value = _draft.value.copy(grade = v) }
    fun updateBoard(v: String) { _draft.value = _draft.value.copy(board = v) }

    fun saveChild() {
        val parentId = auth.currentUser?.uid ?: return
        val editingId = _editingChildId.value
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            val d = _draft.value

            val result = if (editingId != null) {
                val child = Child(
                    id = editingId,
                    name = d.name.trim(),
                    grade = d.grade,
                    board = d.board,
                    parentId = parentId,
                    gender = d.gender.key
                )
                childRepository.updateChild(child)
            } else {
                val child = Child(
                    name = d.name.trim(),
                    grade = d.grade,
                    board = d.board,
                    parentId = parentId,
                    gender = d.gender.key
                )
                childRepository.addChild(child)
            }

            result.onSuccess { saved ->
                _addedChildren.value = if (editingId != null) {
                    _addedChildren.value.map { if (it.id == editingId) saved else it }
                } else {
                    _addedChildren.value + saved
                }
                _draft.value = ChildDraft()
                _editingChildId.value = null
                _step.value = AddChildStep.DONE
            }.onFailure {
                _error.value = it.message ?: "Failed to save. Please try again."
            }
            _isSaving.value = false
        }
    }

    fun addAnotherChild() {
        _draft.value = ChildDraft()
        _editingChildId.value = null
        _step.value = AddChildStep.DETAILS
    }

    fun finish() {
        viewModelScope.launch { _navToParentHome.emit(Unit) }
    }
}
