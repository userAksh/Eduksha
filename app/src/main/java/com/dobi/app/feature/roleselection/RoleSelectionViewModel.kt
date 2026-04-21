package com.dobi.app.feature.roleselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.domain.model.UserRole
import com.dobi.app.domain.usecase.SelectRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoleSelectionViewModel @Inject constructor(
    private val selectRoleUseCase: SelectRoleUseCase
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<UserRole>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun onRoleSelected(role: UserRole) {
        viewModelScope.launch {
            selectRoleUseCase(role)
            _navigationEvent.emit(role)
        }
    }
}
