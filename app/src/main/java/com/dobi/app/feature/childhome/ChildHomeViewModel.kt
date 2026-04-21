package com.dobi.app.feature.childhome

import androidx.lifecycle.ViewModel
import com.dobi.app.domain.model.LearningStatus
import com.dobi.app.domain.model.TodayLearningItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ChildHomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChildHomeUiState(
            childName = "Emma",
            streakDays = 3,
            todayItems = listOf(
                TodayLearningItem(
                    conceptId = "1",
                    conceptName = "Addition of Fractions",
                    subject = "Mathematics",
                    unit = "Fractions",
                    status = LearningStatus.COMPLETED
                ),
                TodayLearningItem(
                    conceptId = "2",
                    conceptName = "Subtraction of Fractions",
                    subject = "Mathematics",
                    unit = "Fractions",
                    status = LearningStatus.COMPLETED
                ),
                TodayLearningItem(
                    conceptId = "3",
                    conceptName = "Water Cycle",
                    subject = "Science",
                    unit = "Our Environment",
                    status = LearningStatus.IN_PROGRESS
                )
            )
        )
    )
    val uiState = _uiState.asStateFlow()
}
