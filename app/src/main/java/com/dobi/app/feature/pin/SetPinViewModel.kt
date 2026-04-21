package com.dobi.app.feature.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetPinViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed class UiState {
        object Entering : UiState()
        object Confirming : UiState()
        object Saving : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Entering)
    val uiState = _uiState.asStateFlow()

    private var firstPin: String = ""

    private val _navDone = MutableSharedFlow<Unit>()
    val navDone = _navDone.asSharedFlow()

    fun onPinEntered(pin: String) {
        if (pin.length != 4) return
        when (_uiState.value) {
            is UiState.Entering -> {
                firstPin = pin
                _uiState.value = UiState.Confirming
            }
            is UiState.Confirming -> {
                if (pin != firstPin) {
                    firstPin = ""
                    _uiState.value = UiState.Error("PINs don't match. Please try again.")
                    return
                }
                savePin(pin)
            }
            is UiState.Error -> {
                firstPin = pin
                _uiState.value = UiState.Confirming
            }
            else -> {}
        }
    }

    private fun savePin(pin: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            authRepository.savePin(pin)
                .onSuccess { _navDone.emit(Unit) }
                .onFailure {
                    firstPin = ""
                    _uiState.value = UiState.Error(it.message ?: "Couldn't save PIN. Try again.")
                }
        }
    }
}
