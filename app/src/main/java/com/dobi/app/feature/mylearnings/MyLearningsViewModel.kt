package com.dobi.app.feature.mylearnings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MyLearningsViewModel @Inject constructor() : ViewModel() {

    // Defaults to empty until the corresponding backends (uploads/quizzes/saved/news) are wired.
    private val _uiState = MutableStateFlow(MyLearningsUiState())
    val uiState = _uiState.asStateFlow()
}
