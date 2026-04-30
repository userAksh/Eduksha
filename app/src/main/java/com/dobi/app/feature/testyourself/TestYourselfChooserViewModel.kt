package com.dobi.app.feature.testyourself

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.GetConceptsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestYourselfChooserUiState(
    val isUnitScope: Boolean = false,
    val unitId: String = "",
    val conceptId: String = "",
    val title: String = "",
    val subject: String = "",
    val subjectColor: String = "",
    val subjectIcon: String = "",
    val unitName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TestYourselfChooserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val getConceptsUseCase: GetConceptsUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.TestYourselfChooser>()
    private val isUnitScope: Boolean = route.conceptId.isBlank()

    private val _uiState = MutableStateFlow(
        TestYourselfChooserUiState(
            isUnitScope = isUnitScope,
            unitId = route.unitId,
            conceptId = route.conceptId
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.linkedChildGrade
                .flatMapLatest { grade -> getConceptsUseCase(grade = grade, board = "CBSE") }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { all ->
                    if (isUnitScope) {
                        val match = all.firstOrNull { it.unitId == route.unitId }
                        if (match == null) {
                            _uiState.update { it.copy(isLoading = false, error = "Lesson not found") }
                        } else {
                            _uiState.update {
                                it.copy(
                                    title = match.unit,
                                    subject = match.subject,
                                    subjectColor = match.subjectColor,
                                    subjectIcon = match.subjectIcon,
                                    unitName = match.unit,
                                    isLoading = false
                                )
                            }
                        }
                    } else {
                        val match = all.firstOrNull { it.id == route.conceptId }
                        if (match == null) {
                            _uiState.update { it.copy(isLoading = false, error = "Concept not found") }
                        } else {
                            _uiState.update {
                                it.copy(
                                    title = match.name,
                                    subject = match.subject,
                                    subjectColor = match.subjectColor,
                                    subjectIcon = match.subjectIcon,
                                    unitId = match.unitId,
                                    unitName = match.unit,
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
        }
    }
}
