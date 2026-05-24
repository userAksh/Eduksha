package com.dobi.app.feature.parentdashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dobi.app.core.navigation.Screen
import com.dobi.app.data.repository.ActivityLogRepository
import com.dobi.app.data.repository.ActivityLogRepository.ActivityLogItem
import com.dobi.app.data.repository.ActivityLogRepository.Source
import com.dobi.app.domain.repository.ChildRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectDetailUiState(
    val isLoading: Boolean = true,
    val subject: String = "",
    val childName: String = "",
    val quizItems: List<ActivityLogItem> = emptyList(),
    val subjectiveItems: List<ActivityLogItem> = emptyList(),
    val classWorkItems: List<ActivityLogItem> = emptyList(),
    val selfLearningItems: List<ActivityLogItem> = emptyList(),
    val homeworkItems: List<ActivityLogItem> = emptyList(),
) {
    val totalCount: Int
        get() = quizItems.size + subjectiveItems.size + classWorkItems.size +
            selfLearningItems.size + homeworkItems.size

    val avgQuizAccuracy: Int?
        get() {
            val scored = quizItems.mapNotNull { it.score }.filter { it.total > 0 }
            if (scored.isEmpty()) return null
            return scored.sumOf { it.correct * 100 / it.total } / scored.size
        }
}

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: FirebaseAuth,
    private val childRepository: ChildRepository,
    private val activityLogRepository: ActivityLogRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.SubjectDetail>()
    val childId: String = route.childId
    val subject: String = route.subject

    private val childName = MutableStateFlow("")

    val uiState = combine(
        activityLogRepository.observeForChild(childId),
        childName
    ) { items, name ->
        val filtered = items.filter { it.subject.equals(subject, ignoreCase = true) }
        SubjectDetailUiState(
            isLoading = false,
            subject = subject,
            childName = name,
            quizItems = filtered.filter { it.source == Source.QUIZ_MCQ },
            subjectiveItems = filtered.filter { it.source == Source.QUIZ_SUBJECTIVE },
            classWorkItems = filtered.filter {
                it.source == Source.UPLOAD && it.uploadKind == "class_work"
            },
            selfLearningItems = filtered.filter {
                it.source == Source.UPLOAD && it.uploadKind == "self_learning"
            },
            homeworkItems = filtered.filter { it.source == Source.HOMEWORK },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubjectDetailUiState(subject = subject),
    )

    init { loadChildName() }

    private fun loadChildName() {
        val uid = auth.currentUser?.uid ?: return
        if (childId.isBlank()) return
        viewModelScope.launch {
            childRepository.getChild(uid, childId)
                .onSuccess { c -> childName.value = c.name.substringBefore(" ") }
        }
    }
}
