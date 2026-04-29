package com.dobi.app.feature.tuitionproblems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TuitionProblemsUiState(
    val selected: Set<String> = emptySet(),
    val otherText: String = "",
    val isSending: Boolean = false,
    val sent: Boolean = false
) {
    val canSend: Boolean get() =
        (selected.isNotEmpty() || otherText.isNotBlank()) && !isSending
}

object TuitionProblems {
    val OPTIONS: List<String> = listOf(
        "Did not understand class",
        "Topic not covered properly",
        "Need help in subject",
        "Doubts not solved",
        "Too crowded, not getting attention"
    )
}

@HiltViewModel
class TuitionProblemsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TuitionProblemsUiState())
    val uiState = _uiState.asStateFlow()

    fun onToggle(option: String) = _uiState.update { state ->
        state.copy(
            selected = if (option in state.selected) state.selected - option
            else state.selected + option
        )
    }

    fun onOtherTextChanged(text: String) = _uiState.update {
        it.copy(otherText = text.take(280))
    }

    fun onSend() {
        if (!_uiState.value.canSend) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            // TODO: persist to Firestore so parents see this in their dashboard.
            delay(400) // brief progress so the kid feels something happened
            _uiState.update { it.copy(isSending = false, sent = true) }
        }
    }
}
